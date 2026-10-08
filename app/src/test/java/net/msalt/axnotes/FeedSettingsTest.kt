package net.msalt.axnotes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FeedSettingsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val settings get() = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    @Before fun reset() { settings.edit().clear().commit() }
    private fun withGraph(test: (AppGraph) -> Unit) {
        val graph = AppGraph(context)
        try { test(graph) } finally { graph.contentDb.close(); graph.personalDb.close() }
    }
    @Test fun freshInternalInstallUsesLiveFeed() = withGraph { graph ->
        assertFalse(graph.sampleMode)
        assertFalse(graph.content().sample)
        assertEquals("https://ax.msalt.net/app/v1/manifest.json", graph.content().manifestUrl)
    }
    @Test fun previousInternalSamplePreferenceMigratesWithoutChangingOtherSettings() {
        settings.edit().putBoolean("sample", true).putString("unrelated", "keep").commit()
        withGraph { assertFalse(it.sampleMode) }
        assertEquals("keep", settings.getString("unrelated", null))
        assertTrue(settings.getBoolean("liveFeedDefaultV2", false))
    }
    @Test fun explicitFixtureChoiceAfterMigrationSurvivesRecreation() {
        withGraph { it.sampleMode = true }
        withGraph { assertTrue(it.sampleMode) }
    }
}
