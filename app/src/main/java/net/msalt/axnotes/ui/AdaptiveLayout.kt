package net.msalt.axnotes.ui

/** Window-space policy, independent of orientation/device labels and testable without Android. */
internal data class AdaptiveLayout(val useRail: Boolean, val useTwoPanes: Boolean, val listWidthDp: Float)

internal fun adaptiveLayout(widthDp: Float, heightDp: Float, fontScale: Float): AdaptiveLayout {
    val enlargedText = fontScale >= 1.5f
    val listWidth = if (enlargedText) 380f else (widthDp * .32f).coerceIn(320f, 400f)
    return AdaptiveLayout(
        useRail = widthDp >= 600f,
        useTwoPanes = widthDp >= (if (enlargedText) 1000f else 840f) && heightDp >= 480f,
        listWidthDp = listWidth
    )
}

