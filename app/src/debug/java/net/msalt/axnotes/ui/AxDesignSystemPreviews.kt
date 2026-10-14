package net.msalt.axnotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Android Studio renders the production components; there is no mock HTML replica. */
@Preview(name = "AX · light · compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun AxLightCatalogPreview() = AxDesignSystemCatalog(darkTheme = false)

@Preview(name = "AX · dark · compact", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun AxDarkCatalogPreview() = AxDesignSystemCatalog(darkTheme = true)

@Preview(name = "AX · large text · 200%", widthDp = 360, heightDp = 800, fontScale = 2f, showBackground = true)
@Composable
private fun AxLargeTextCatalogPreview() = AxDesignSystemCatalog(darkTheme = false)

@Preview(name = "AX · expanded", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
private fun AxExpandedCatalogPreview() = AxDesignSystemCatalog(darkTheme = false)
