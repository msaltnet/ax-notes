# AX Notes design system

Decision date: 2026-10-09. Selected before the issue #6 interface changes.

## Selection

Use Google's **Material 3 Design Kit**, available in [Figma Community](https://www.figma.com/community/file/1035203688168086460/material-3-design-kit), as the design reference. AX Notes is an Android-only Kotlin/Compose application and already uses `androidx.compose.material3:material3`. The [official Compose guidance](https://developer.android.com/develop/ui/compose/designsystems/material3) provides the corresponding component and theme implementation. This keeps design roles and code aligned without another UI dependency.

Compared with:

- **Figma Simple Design System:** a useful token/component foundation, but its [official repository](https://github.com/figma/sds) presents it with a React codebase for responsive web interfaces. It would require additional Android-specific translation.
- **Apple UI kits:** strong native resources for Apple's platforms, as shown by [Apple Design Resources](https://developer.apple.com/design/resources/). Their platform patterns are a weaker fit for this Android-only application.

[Figma's official UI-kit help](https://help.figma.com/hc/en-us/articles/24037724065943-Start-designing-with-UI-kits) lists Material 3, Simple Design System and Apple kits, and says UI kits are available on all plans to editors. This comparison uses primary sources, not an assessment of downloaded Community files.

## Figma installation status

The reference has been selected and mapped to the app's existing Compose implementation. **No Figma file was created, duplicated or edited, and no Figma library was installed or enabled.** The Figma connection was not installed in this session. The direct Community page could not be rendered by the research tool; its identity and availability were verified through Figma's official help page. No node-level inspection or design-file synchronization is claimed.

For a future authorized Figma setup, open the intended design file, choose **Assets → Libraries → UI kits → Material 3 Design Kit → Add to file**, then apply the AX tokens below. Figma access and a target file are still needed to verify that step. Enabling the kit is distinct from using the already-installed Compose Material 3 dependency.

## AX adaptation and token mapping

`app/src/main/java/net/msalt/axnotes/ui/AxDesignSystem.kt` is the app's theme/token source. `AxTheme` supplies explicit light/dark color roles, shared typography and shape sizes. Dynamic wallpaper colors are intentionally not used so the calm green/ivory identity remains consistent.

| Role | AX value | Compose mapping |
| --- | --- | --- |
| Main accent | Light `#315E50`; dark `#B3D5C3` | `colorScheme.primary` |
| Reading background | Light ivory `#FAF8F0`; dark green-black `#151D19` | `background` / `surface` |
| Main text | Light `#202820`; dark `#E3E8DF` | `onSurface` / `onBackground` |
| Secondary text | Light `#515C52`; dark `#C3CCC0` | `onSurfaceVariant` |
| Selected item | Light `#DFE9DF`; dark `#3B5141` | `secondaryContainer` with its paired foreground |
| Cards and grouping | Tonal surfaces rather than strong shadows | `surfaceContainerLow` / `surfaceContainer` |
| Spacing scale | 4, 8, 12, 16, 20, 24, 32dp | `AxSpacing.xs` through `section` |
| Page margin | 20dp compact; 24dp when space permits | `AxSpacing.xl` / `xxl` |
| Card radius | 16dp | `AxSize.cardRadius` / `shapes.large` |
| Body text | 16sp/26sp large; 14sp/22sp medium | `bodyLarge` / `bodyMedium` |
| Metadata | 12sp/18sp | `bodySmall` / `labelMedium` |
| Title hierarchy | 20/28, 24/32 and 28/36sp | `titleLarge`, `headlineMedium`, `headlineLarge` |
| Icon viewport | 24dp | `AxSize.icon` |
| Minimum interactive target | 48dp | `AxSize.minTouch` |
| Compact bottom navigation | 56dp content height, plus system bottom inset | `AxSize.bottomNavigation` |

Typography uses Android's sans-serif fallback, including Korean glyphs, and scalable `sp` units. Reading text is left-aligned, with relaxed line height; important titles can wrap rather than compete with actions. Structural spacing follows a 4dp base and uses 8dp multiples for larger groups. Keep a 16dp inset within cards and 12–16dp between neighboring cards. A thin border or tonal fill communicates grouping; repeated heavy shadows are unnecessary.

## Components and accessibility

- Article, bookmark and memo cards share shape, padding and text hierarchy. Their content and actions remain distinct.
- Buttons, chips, text fields, dialogs and navigation-rail controls use the existing Material 3 Compose components and the shared theme. Do not add another UI framework.
- The requested 56dp icon-only bottom bar is an **AX-specific density adaptation**, not a claim that 56dp is the stock Material 3 navigation-bar height. Keep each icon's clickable area at least 48 × 48dp, with a visible selected treatment, selected/tab semantics and a spoken destination label. Removing visible labels must not remove accessibility labels.
- Keep system navigation/gesture insets outside the 56dp bar content. Rail and two-pane layouts continue to follow the existing adaptive policy in `ADAPTIVE_DESIGN.md`.
- Icons use a consistent 24dp viewport. Personal storage uses an archive-box silhouette, distinct from the Notes glyph; bookmarks retain their bookmark shape. Tint navigation vectors through the theme.
- The app icon retains a green notebook cover and binding, with a hand-defined **AX** monogram rather than generic horizontal text strokes. It uses vector geometry and no font dependency.
- Font scaling, keyboard focus, TalkBack navigation, dark-mode contrast, small-screen wrapping and real-device touch behavior still require verification against the assembled app; defining tokens alone does not prove those flows.

## Version identity

The candidate is `0.3.0`, versionCode `3`. The debug package remains `net.msalt.axnotes.internal` so this change does not rename the installed application's data namespace. `VERSION_NAME` no longer adds an `-internal` suffix; this presentation change does not make the build a production release. An in-place update still requires the same signing key.

## Token and asset checks

Fresh checks of the restored files ran on 2026-10-09 at 23:48 UTC. A static WCAG relative-luminance calculation checked 24 light/dark text pairs: primary, secondary, tertiary, their containers, main/secondary surface text, secondary text on low/default containers, and error roles. All exceeded 4.5:1; the lowest was 5.98:1. This checks these token pairs, not every rendered state or reduced-opacity control in the app.

Both changed Android vector XML files parsed successfully and were rendered again to PNG for visual review: the notebook's AX monogram and the archive box were legible. Static version assertions verified 0.3.0/code 3, the retained debug package suffix and the absent version suffix. `git diff --check` passed. Android compilation, app-level tests and device UI results must be recorded separately after integration; vector previews are not Android screenshots.
