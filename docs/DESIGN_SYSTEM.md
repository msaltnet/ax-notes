# AX Notes design system

Reference selected on 2026-10-09; implementation mapping reviewed on 2026-10-10 (UTC). This specification covers the Android 0.3.0 candidate and its reading, linked-memo, bookmark and publisher-classification flows.

## Decision and source comparison

Use Google's **Material 3 Design Kit** as the design-system reference, then express AX's reading-oriented choices in the existing Compose Material 3 implementation. The app already uses `androidx.compose.material3:material3`; another UI framework would add translation and regression work without improving the core reading flow.

| Candidate | Verified original | Fit for AX Notes | Decision |
| --- | --- | --- | --- |
| Google Material 3 Design Kit | [Figma Community file](https://www.figma.com/community/file/1035203688168086460/material-3-design-kit), linked by [Figma's official UI-kit guide](https://help.figma.com/hc/en-us/articles/24037724065943-Start-designing-with-UI-kits) | Android component families and theme roles match the existing Compose code. [Google's account of the kit](https://design.google/library/config-2024) confirms Compose code snippets and prototyping support. | Selected foundation; AX owns its palette, density, reader styling and product compositions. |
| Figma Simple Design System | [Figma Community file](https://www.figma.com/community/file/1380235722331273046/simple-design-system), linked by [Figma's own repository](https://github.com/figma/sds) | Useful variable/component organization and responsive-web examples. Its reference implementation is React, requiring another Android behavior mapping. | Considered, not imported or ported. |

These are product-fit judgments based on primary documentation, not a claim that the current Community files were downloaded or inspected node by node. The reference is the actual Google-published Figma kit, not an unrelated Material-styled template.

Figma documents both as UI kits available to editors on all plans. Basic use therefore has a documented path without buying another kit. Code Connect has separate paid-plan requirements and is not required by this implementation. Connected UI kits can offer upstream updates; copied files do not automatically inherit them. No purchase, subscription or upgrade was made.

## What exists, and what does not

The repository contains an AX theme, reusable production components, reader tokens, a component catalogue, debug preview declarations and a generated token handoff. These are implemented code artifacts.

**No Figma design file was created, duplicated or edited. No Figma variables collection, component set or library was created, installed, enabled, published or synchronized.** The Community page was unavailable in the cloud browser, and direct research retrieval was blocked; its identity was established through official source links. Figma account editing access and write capabilities were not verified during this work. No node IDs, asset extraction or Figma-to-code synchronization are claimed.

Enabling a reference kit and building an AX library are separate steps. A future Figma handoff needs an editable destination, inspection of the supported editing tools, AX light/dark variables, reusable component variants, representative product screens and review of their behavior. Preserve linked upstream instances where their exposed properties support AX customization. If components are copied for local customization, record the source and maintain updates explicitly. A screen composed of editable layers alone is not a completed component library.

The [generated token JSON](../design/ax-tokens.json) is a handoff reference, **not a verified Figma import/plugin format**. It must not be presented as a completed Figma installation.

## Source of truth and three-layer mapping

- [AxDesignSystem.kt](../app/src/main/java/net/msalt/axnotes/ui/AxDesignSystem.kt): explicit light/dark schemes, all 15 typography roles, spacing, shapes, dimensions and component tokens.
- [AxComponents.kt](../app/src/main/java/net/msalt/axnotes/ui/AxComponents.kt): reusable components used by the app, built on Material controls.
- [AxReaderStyle.kt](../app/src/main/java/net/msalt/axnotes/ui/AxReaderStyle.kt): shared long-form typography/layout and generated HTML colors.
- [AxDesignSystemCatalog.kt](../app/src/main/java/net/msalt/axnotes/ui/AxDesignSystemCatalog.kt): executable catalogue of those production components, not a separate mock UI or app destination.

The three layers describe how values reach the UI; they do not imply three existing Figma collections:

1. **Foundation values:** concrete light/dark colors, spacing, type sizes and shape/dimension values in Kotlin. Color literals are defined directly in Material role declarations; there is no claimed imported Google tonal-palette database.
2. **Semantic roles:** `MaterialTheme.colorScheme`, `typography` and `shapes` express usage. The JSON also provides `canvas`, `text`, `supportingText`, `action`, `card`, `selected` and `error` aliases to those colors.
3. **Component decisions:** `AxComponentTokens`, relevant `AxSize` values and implementations select padding, outlines, control shapes and role pairs. Ordinary cards use `surfaceContainerLow/onSurface`; selected cards use `secondaryContainer/onSecondaryContainer` and a stronger primary-colored outline.

Change Kotlin, regenerate with `python scripts/export-design-tokens.py`, then run `python scripts/export-design-tokens.py --check`. Do not maintain an unrelated HTML palette or manually edited token export. See [design/README.md](../design/README.md). The [official Compose theme model](https://developer.android.com/develop/ui/compose/designsystems/material3) supplies the upstream color/type/shape mapping.

## AX color roles

The existing forest-green and paper palette is retained. `AxTheme` follows the system light/dark setting but intentionally does not apply wallpaper-derived dynamic colors.

| AX use | Material role | Light | Dark |
| --- | --- | --- | --- |
| Primary action / link | `primary` | `#315E50` | `#B3D5C3` |
| Text on primary action | `onPrimary` | `#FFFFFF` | `#113829` |
| Primary container / foreground | `primaryContainer/onPrimaryContainer` | `#D5E9DB / #163C2E` | `#2D5141 / #D5E9DB` |
| Canvas / reading surface | `background/surface` | `#FAF8F0` | `#151D19` |
| Main text | `onSurface/onBackground` | `#202820` | `#E3E8DF` |
| Supporting text | `onSurfaceVariant` | `#515C52` | `#C3CCC0` |
| Ordinary card | `surfaceContainerLow` | `#F5F3EB` | `#1A241D` |
| Selected container / foreground | `secondaryContainer/onSecondaryContainer` | `#DFE9DF / #243D30` | `#3B5141 / #DFE9DF` |
| Subtle border | `outlineVariant` | `#CDD3C7` | `#424D43` |
| Error container / foreground | `errorContainer/onErrorContainer` | `#F9DEDC / #410E0B` | `#8C1D18 / #F9DEDC` |

Information notices use the secondary container pair, warnings the tertiary pair, and errors the error pair. Destructive text actions use `error`. Important state also uses text, icons, outlines or semantics. A decorative outline is not a substitute for a focus or selection indicator.

The complete scheme, including inverse and surface-container roles, lives in Kotlin and the JSON. Pair foregrounds with their intended backgrounds. Static color-pair checks alone do not establish the contrast of overlays, disabled controls, images or every rendered screen.

## Typography, spacing and shape

| Use | Current AX value | Source |
| --- | --- | --- |
| Screen title | 22sp / 28sp, semibold | `typography.titleLarge` |
| Content-card title | 16sp / 24sp, medium | `typography.titleMedium` |
| UI body | 16sp / 24sp | `typography.bodyLarge` |
| Supporting text | 14sp / 20sp | `typography.bodyMedium` |
| Metadata / medium label | 12sp / 16sp | `bodySmall/labelMedium` |
| Long-form reader | 18 / 30 | `AxReaderStyle`; units below |
| Spacing scale | 4, 8, 12, 16, 20, 24, 32dp | `AxSpacing` |
| Page margin | 20dp | `AxComponentTokens.pageMargin`; AX density choice |
| Card padding / card gap | 16dp / 12dp | `AxComponentTokens` |
| Section gap / control gap | 24dp / 8dp | `AxComponentTokens` |
| Shape scale | 4, 8, 12, 16, 28dp | `AxShapes` |
| Card / dialog corners | 16dp / 28dp | `AxSize` |
| Button / input / filter corners | 12dp / 12dp / 8dp | `AxSize` |
| Ordinary / selected card outline | 1dp / 2dp | `AxComponentTokens` |
| Icon / minimum interactive target | 24dp / 48 × 48dp | `AxSize` and Material control sizing |
| Field / editor minimum height | 56dp / 200dp | `AxSize` |

All Compose text roles specify Android's sans-serif fallback, scalable `sp` sizes and zero letter spacing; Korean fallback remains platform-provided. No font package was added. Reader text is a separate long-form style rather than an enlargement of every UI label. Controls/forms use minimum heights and wrapping/scrolling. The token named `AxSpacing.section` is 32dp; the **component** `sectionGap` intentionally aliases the 24dp step.

These are AX code values, not Figma node measurements. Compact navigation, green/paper colors, reader metrics and control corners are deliberate product adaptations.

## Production components and state contracts

| AX component | Material foundation | Implemented variants and behavior |
| --- | --- | --- |
| `AxCard` | Clickable `Card` | Default/selected, paired colors, 1/2dp outline and selected semantics. Used for articles and publisher collections. |
| `AxCardFrame` | Non-clickable `Card` | Default/selected container for saved records with independent body and secondary actions. The frame does not intercept their clicks. |
| `AxButton` | `Button` | Primary, enabled/disabled/loading. Loading blocks duplicate invocation, retains content, adds progress and exposes a polite “처리 중” state. The memo Save call site uses full width. |
| `AxOutlinedButton` | `OutlinedButton` | Secondary, enabled/disabled; used for date selection and cache management. |
| `AxTextButton` | `TextButton` | Low-emphasis/destructive, enabled/disabled. Destructive color complements an explicit label and existing confirmation. |
| `AxFilterChip` | `FilterChip` | Selected/unselected/disabled, selected check mark, selection semantics and paired colors; minimum 48 × 48dp. |
| `AxTextField` | `OutlinedTextField` | Empty/filled/error/read-only/disabled, optional labels/supporting text/icons. Error gets a warning icon unless the caller supplies a trailing icon. Single/multiline behavior is caller-controlled. |
| `AxNotice` | `Surface`, icon and text | Information/warning/error using separate role pairs and visible messages. |
| `AxEmptyState` | Text composition | Title and specific next-step guidance. Actions remain with the owning screen. |

Focus, hover where supported, press/ripple and native disabled behavior come from the installed Material controls. There is no separate interaction engine or copied opacity table. Domain card content must still provide adequate target bounds; a generic empty `AxCard` does not enforce a 48dp minimum.

These components serve Notes, Library, search, reader actions, memo editing, settings and dialogs. Article titles/excerpts and memo summaries have intentional bounded previews; full content remains in the reader/editor. Bookmark state uses the existing icon-and-label action with spoken state, not a new `AxIconToggle` class.

## Product meaning and adaptive layout

- Notes classification follows publisher-authored **series/project** IDs, titles and order. This work does not introduce a category hierarchy, free-form tag editor or a new data model. Missing taxonomy must not invent membership from titles.
- Bookmarks, linked memos, reading reminders and cached bodies remain distinct. “본문 저장됨” means cached text, not a bookmark or completed-reading flag.
- Empty/loading/unavailable/offline/retry flows remain screen-owned. A catalogue error notice is not evidence that every application failure uses it. Preserve draft/discard protection, selection and reading/list position.
- Compact navigation remains the requested **56dp icon-only content bar plus system inset**. This is an AX adaptation, not the stock Material navigation height. Retain spoken labels, selected/tab semantics and at least 48dp interactive targets.
- Below 600dp: one pane and compact navigation. At 600dp: a scrollable rail. At 840dp and at least 480dp height: list/detail. At 150%+ text, the two-pane threshold becomes 1000dp. Rail width is 96dp normally and 112dp at large text. See [ADAPTIVE_DESIGN.md](ADAPTIVE_DESIGN.md) for the existing policy.
- Reader outer space is capped at **880dp**. HTML has a centered **764 CSS-pixel border-box column**, including responsive 18–32px horizontal padding. Native fallback has the corresponding **764dp** cap. Other single-pane pages use the 760dp cap. The earlier exploratory 680dp suggestion is not the implemented contract.

Reader font sizes use CSS pixels in WebView and `sp` in native fallback; layouts use CSS pixels and `dp`, respectively. HTML leading is a unitless ratio so 18/30 scales with text zoom. Native fallback also updates for theme and font scale. Images fit the column; code/tables can scroll horizontally. Active Material roles supply HTML background, body/link/supporting text and code surfaces. Sanitized content, restrictive WebView settings and the content-security policy are retained.

The notebook/AX launcher icon and archive-box Library icon remain the candidate's vector assets. Their presence does not imply imported Figma artwork.

## Catalogue, export and verification scope

[Debug Compose previews](../app/src/debug/java/net/msalt/axnotes/ui/AxDesignSystemPreviews.kt) declare 360dp light/dark, 200% text and 840dp catalogue configurations, using production components. Tooling dependencies are debug-only and use the existing Compose BOM. Declaring previews is not evidence that Android Studio rendered them or that a device was exercised.

Automated contracts are defined in:

- [AxDesignSystemTest](../app/src/test/java/net/msalt/axnotes/ui/AxDesignSystemTest.kt): paired-color contrast, explicit type roles, scalable units, spacing, shapes and component dimensions.
- [AxComponentsTest](../app/src/test/java/net/msalt/axnotes/ui/AxComponentsTest.kt): component semantics, target bounds, repeated/disabled/loading actions, input/selection, independent saved-card actions and long Korean text at 200% with native font measurement under Robolectric.
- [AxReaderStyleTest](../app/src/test/java/net/msalt/axnotes/AxReaderStyleTest.kt): theme mapping, HTML typography/layout, unitless leading, escaping/security policy and fallback updates.
- Existing app/adaptive suites: navigation, drafts, selection, bookmarks/memos/reminders and window changes.

Run `python scripts/export-design-tokens.py --check` for drift, then the repository aggregate `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`. Results and the exact candidate belong in [RELEASE_0.3.0.md](RELEASE_0.3.0.md); no pending aggregate is treated as passed here. Earlier results do not automatically cover the follow-up.

Device/emulator WebView rendering, images, TalkBack, keyboard/touch behavior, fold hinges and performance still need device evidence. A static contrast test, Robolectric flow, compiled instrumentation APK or vector preview is not physical-device certification. [Android accessibility guidance](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) informs control checks; [WCAG text contrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum) explains the 4.5:1 normal-text threshold.

## Attribution and licensing boundaries

Design reference: **Material 3 Design Kit, Google / Material Design**, [original Community resource](https://www.figma.com/community/file/1035203688168086460/material-3-design-kit). AX's local work defines its palette, density, type adaptation, reader presentation and domain compositions. No Community design assets were downloaded or copied during this work.

[Figma's Community licensing policy](https://help.figma.com/hc/en-us/articles/360042296374-Figma-Community-copyright-and-licensing) states that free Community files use CC BY 4.0 and require creator attribution; creators may offer additional licenses. This is the verified **general policy**, not a claim that this session inspected the selected file's individual license panel. Confirm current resource-specific terms before importing or redistributing assets, and retain creator/source/license/change notices as applicable. [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) is linked for that attribution workflow.

The comparator's [SDS code license](https://github.com/figma/sds/blob/main/LICENSE) is MIT, but no SDS code is incorporated. Code dependencies and design resources have separate obligations. This document does **not** select, replace or declare an AX Notes repository license, or assert that Google's code and every Figma asset share one license.

## Version and installation identity

This extends the same unpublished `0.3.0` candidate, versionCode `3`. Debug application ID remains `net.msalt.axnotes.internal`; removing the visible version suffix does not rename the data namespace or make a production release. In-place updates still require the installed signing key. No release, uninstall, data reset or device installation is part of this design-system documentation.
