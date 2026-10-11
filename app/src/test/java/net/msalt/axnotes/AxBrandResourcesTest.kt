package net.msalt.axnotes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import androidx.test.core.app.ApplicationProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.xmlpull.v1.XmlPullParser

/** Packaged brand assets must stay usable offline and survive launcher masks. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AxBrandResourcesTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val androidNs = "http://schemas.android.com/apk/res/android"

    @Test
    fun allBundledFontFacesHaveTheCorrectFamiliesWeightsAndFullKoreanCoverage() {
        val fonts = listOf(
            Triple(R.font.noto_serif_kr_semibold, "Noto Serif KR", 600),
            Triple(R.font.ibm_plex_sans_kr_regular, "IBM Plex Sans KR", 400),
            Triple(R.font.ibm_plex_sans_kr_medium, "IBM Plex Sans KR", 500),
            Triple(R.font.ibm_plex_sans_kr_semibold, "IBM Plex Sans KR", 600),
            Triple(R.font.pretendard_variable, "Pretendard", 400)
        )
        fonts.forEach { (resource, family, weight) ->
            val name = context.resources.getResourceEntryName(resource)
            assertNotNull("$name must load without downloading a font", ResourcesCompat.getFont(context, resource))
            val font = Sfnt(context.resources.openRawResource(resource).use { it.readBytes() })
            assertTrue("$name must contain its real family name", font.familyNames().any { it.contains(family) })
            assertEquals("$name must declare its intended weight", weight, font.weight())
            (0xAC00..0xD7A3).forEach { code ->
                assertTrue("$name is missing modern Hangul U+${code.toString(16)}", font.hasGlyph(code))
            }
            "AX NOTES0123456789가나다라마바사아자차카타파하·–.,!?()".forEach { character ->
                assertTrue("$name is missing UI glyph $character", font.hasGlyph(character.code))
            }
            if (resource == R.font.pretendard_variable) {
                val (minimum, default, maximum) = font.weightAxis()
                assertEquals(400f, default, 0f)
                assertTrue("The body font must supply the complete CSS100–900 range", minimum <= 100f && maximum >= 900f)
            } else {
                assertFalse("$name is a static packaged face, not a variable or synthesized substitute", font.hasTable("fvar"))
            }
        }
    }

    @Test
    fun eachOfflineFontFamilyHasItsDistributionLicense() {
        val licenses = listOf("NotoSerifKR-OFL.txt", "IBMPlexSansKR-OFL.txt", "Pretendard-OFL.txt")
        licenses.forEach { name ->
            val notice = context.assets.open("licenses/$name").bufferedReader().use { it.readText() }
            assertTrue("$name must include the full OFL", notice.contains("SIL OPEN FONT LICENSE", ignoreCase = true))
            assertTrue(notice.contains("Version 1.1", ignoreCase = true))
            assertTrue("$name must retain redistribution conditions", notice.contains("PERMISSION & CONDITIONS"))
        }
    }

    @Test
    fun applicationUsesTheAdaptiveLauncherAndOpaqueWebsiteBackground() {
        assertEquals(R.mipmap.ic_launcher, context.applicationInfo.icon)
        context.assets.openXmlResourceParser("AndroidManifest.xml").use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "application") {
                    assertEquals(R.mipmap.ic_launcher, parser.getAttributeResourceValue(androidNs, "roundIcon", 0))
                    break
                }
                parser.next()
            }
            assertEquals("The packaged manifest must contain its application", "application", parser.name)
        }
        assertEquals(Color.rgb(0x19, 0x19, 0x15), context.getColor(R.color.ax_icon_background))
        val adaptive = context.getDrawable(R.mipmap.ic_launcher)
        assertTrue(adaptive is AdaptiveIconDrawable)
        val layers = adaptiveLayers()
        assertEquals(R.color.ax_icon_background, layers["background"])
        assertEquals(R.drawable.ic_launcher_foreground, layers["foreground"])
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        (adaptive as AdaptiveIconDrawable).background.apply { setBounds(0, 0, 108, 108); draw(Canvas(bitmap)) }
        for (point in listOf(0 to 0, 107 to 0, 0 to 107, 107 to 107, 54 to 54)) {
            assertEquals("Launcher background must remain fully opaque", Color.rgb(0x19, 0x19, 0x15), bitmap.getPixel(point.first, point.second))
        }
    }

    @Test
    @Config(sdk = [33])
    fun androidThirteenLauncherUsesTheMatchingMonochromeMark() {
        assertEquals(R.drawable.ic_launcher_monochrome, adaptiveLayers()["monochrome"])
        val adaptive = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        assertNotNull("Themed launchers must receive a monochrome drawable", adaptive.monochrome)
    }

    @Test
    fun launcherForegroundAndMonochromeKeepTheExactWebsiteAxGeometryInsideTheSafeCircle() {
        val expectedPaths = listOf(
            "M13 0L13 33L98 44L331 745L419 745L658 41L736 33L736 0L412 0L412 33L514 44L450 241L208 241L146 44L240 33L240 0ZM440 275L328 622L219 275Z",
            "M12 0L12 33L101 44L301 345L93 701L22 708L22 741L350 741L350 708L250 698L394 452L543 696L446 708L446 741L685 741L685 708L592 697L413 421L635 41L712 33L712 0L378 0L378 33L478 42L320 313L156 46L255 33L255 0Z"
        )
        val foreground = vectorPaths(R.drawable.ic_launcher_foreground)
        val monochrome = vectorPaths(R.drawable.ic_launcher_monochrome)
        assertEquals(expectedPaths, foreground.map { it.data })
        assertEquals(expectedPaths, monochrome.map { it.data })
        assertEquals(expectedPaths, vectorPaths(R.drawable.ax_brand_mark, expectedViewport = null).map { it.data })
        foreground.forEachIndexed { index, glyph ->
            val bounds = RectF().also { glyph.path.computeBounds(it, true) }
            val monoBounds = RectF().also { monochrome[index].path.computeBounds(it, true) }
            assertEquals(bounds, monoBounds)
            assertEquals(Color.rgb(0xF3, 0xF0, 0xE7), glyph.color)
            assertEquals(Color.WHITE, monochrome[index].color)
            // All vertices of these straight-edged glyphs are contained by this
            // bounding rectangle. Its corners fitting proves the full path fits.
            listOf(bounds.left to bounds.top, bounds.right to bounds.top,
                bounds.left to bounds.bottom, bounds.right to bounds.bottom).forEach { (x, y) ->
                assertTrue("AX glyph must stay inside the central66dp safe circle: $bounds",
                    (x - 54f) * (x - 54f) + (y - 54f) * (y - 54f) <= 33f * 33f)
            }
        }
        val combined = RectF().also { foreground.first().path.computeBounds(it, true) }
        foreground.drop(1).forEach { glyph -> combined.union(RectF().also { glyph.path.computeBounds(it, true) }) }
        assertEquals(27.343f, combined.left, 0.002f)
        assertEquals(80.657f, combined.right, 0.002f)
        assertEquals(36.865f, combined.top, 0.002f)
        assertEquals(71.135f, combined.bottom, 0.002f)
        val left = RectF().also { foreground[0].path.computeBounds(it, true) }
        val right = RectF().also { foreground[1].path.computeBounds(it, true) }
        assertTrue("The source's overlappingAX silhouette must be retained", RectF.intersects(left, right))
    }

    @Test
    fun launcherMarksHaveTransparentMarginsAndMatchingRasterSilhouettes() {
        fun render(resource: Int) = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888).also { bitmap ->
            context.getDrawable(resource)!!.apply { setBounds(0, 0, bitmap.width, bitmap.height); draw(Canvas(bitmap)) }
        }
        val foreground = render(R.drawable.ic_launcher_foreground)
        val monochrome = render(R.drawable.ic_launcher_monochrome)
        var inkPixels = 0
        for (y in 0 until foreground.height) for (x in 0 until foreground.width) {
            val alpha = Color.alpha(foreground.getPixel(x, y))
            assertEquals("Both launchers must use the same silhouette at($x,$y)", alpha, Color.alpha(monochrome.getPixel(x, y)))
            if (alpha > 0) {
                inkPixels++
                val dx = (x + 0.5f) / 4f - 54f
                val dy = (y + 0.5f) / 4f - 54f
                assertTrue("Rendered launcher ink must remain mask-safe", dx * dx + dy * dy <= 33f * 33f)
            }
        }
        assertTrue("The mark must not silently become blank", inkPixels > 1_000)
        assertEquals(0, Color.alpha(foreground.getPixel(0, 0)))
        assertEquals(0, Color.alpha(foreground.getPixel(431, 431)))
    }

    private fun adaptiveLayers(): Map<String, Int> {
        val layers = mutableMapOf<String, Int>()
        context.resources.getXml(R.mipmap.ic_launcher).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name in listOf("background", "foreground", "monochrome")) {
                    layers[parser.name] = parser.getAttributeResourceValue(androidNs, "drawable", 0)
                }
                parser.next()
            }
        }
        return layers
    }

    private data class Glyph(val data: String, val color: Int, val path: Path)
    private fun vectorPaths(resource: Int, expectedViewport: Float? = 108f): List<Glyph> {
        val paths = mutableListOf<Glyph>()
        val transforms = mutableListOf(Matrix())
        context.resources.getXml(resource).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                    "vector" -> if (expectedViewport != null) {
                        assertEquals(expectedViewport, parser.getAttributeFloatValue(androidNs, "viewportWidth", 0f))
                        assertEquals(expectedViewport, parser.getAttributeFloatValue(androidNs, "viewportHeight", 0f))
                    }
                    "group" -> {
                        val local = Matrix().apply { setValues(floatArrayOf(
                            parser.getAttributeFloatValue(androidNs, "scaleX", 1f), 0f, parser.getAttributeFloatValue(androidNs, "translateX", 0f),
                            0f, parser.getAttributeFloatValue(androidNs, "scaleY", 1f), parser.getAttributeFloatValue(androidNs, "translateY", 0f),
                            0f, 0f, 1f
                        )) }
                        transforms += Matrix(transforms.last()).apply { preConcat(local) }
                    }
                    "path" -> {
                        val data = parser.getAttributeValue(androidNs, "pathData")
                        val color = parser.getAttributeIntValue(androidNs, "fillColor", 0)
                        paths += Glyph(data, color, PathParser.createPathFromPathData(data)!!.apply { transform(transforms.last()) })
                    }
                }
                if (parser.eventType == XmlPullParser.END_TAG && parser.name == "group") transforms.removeAt(transforms.lastIndex)
                parser.next()
            }
        }
        return paths
    }

    /** Reads SFNT metadata/cmap directly so device fallback fonts cannot hide a missing Korean glyph. */
    private class Sfnt(private val bytes: ByteArray) {
        private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        private val tables = (0 until u16(4)).associate { index ->
            val record = 12 + index * 16
            String(bytes, record, 4, Charsets.US_ASCII) to u32(record + 8)
        }
        private val cmaps: List<Int> = tables.getValue("cmap").let { cmap ->
            (0 until u16(cmap + 2)).map { cmap + u32(cmap + 4 + it * 8 + 4) }
                .distinct().filter { u16(it) == 4 || u16(it) == 12 }
        }
        fun hasTable(tag: String) = tables.containsKey(tag)
        fun weight() = u16(tables.getValue("OS/2") + 4)
        fun familyNames(): List<String> {
            val name = tables.getValue("name")
            val strings = name + u16(name + 4)
            return (0 until u16(name + 2)).mapNotNull { index ->
                val record = name + 6 + index * 12
                if (u16(record + 6) !in listOf(1, 16)) return@mapNotNull null
                val platform = u16(record)
                val charset = if (platform == 0 || platform == 3) Charsets.UTF_16BE else Charsets.ISO_8859_1
                String(bytes, strings + u16(record + 10), u16(record + 8), charset)
            }
        }
        fun weightAxis(): Triple<Float, Float, Float> {
            val fvar = tables.getValue("fvar")
            val axes = fvar + u16(fvar + 4)
            val axis = (0 until u16(fvar + 8)).map { axes + it * u16(fvar + 10) }
                .single { String(bytes, it, 4, Charsets.US_ASCII) == "wght" }
            fun fixed(at: Int) = buffer.getInt(at) / 65536f
            return Triple(fixed(axis + 4), fixed(axis + 8), fixed(axis + 12))
        }
        fun hasGlyph(code: Int): Boolean = cmaps.any { cmap ->
            if (u16(cmap) == 12) {
                var low = 0
                var high = u32(cmap + 12) - 1
                var found = false
                while (low <= high) {
                    val middle = (low + high) / 2
                    val group = cmap + 16 + middle * 12
                    when {
                        code < u32(group) -> high = middle - 1
                        code > u32(group + 4) -> low = middle + 1
                        else -> { found = u32(group + 8) + code - u32(group) != 0; break }
                    }
                }
                found
            } else {
                val segments = u16(cmap + 6) / 2
                val end = cmap + 14
                val start = end + 2 * segments + 2
                val delta = start + 2 * segments
                val ranges = delta + 2 * segments
                var low = 0
                var high = segments - 1
                while (low < high) {
                    val middle = (low + high) / 2
                    if (u16(end + 2 * middle) < code) low = middle + 1 else high = middle
                }
                val index = low
                if (code !in u16(start + 2 * index)..u16(end + 2 * index)) false else {
                    val range = u16(ranges + 2 * index)
                    val glyph = if (range == 0) (code + u16(delta + 2 * index)) and 0xffff
                    else u16(ranges + 2 * index + range + 2 * (code - u16(start + 2 * index))).let {
                        if (it == 0) 0 else (it + u16(delta + 2 * index)) and 0xffff
                    }
                    glyph != 0
                }
            }
        }
        private fun u16(at: Int) = buffer.getShort(at).toInt() and 0xffff
        private fun u32(at: Int) = buffer.getInt(at)
    }
}
