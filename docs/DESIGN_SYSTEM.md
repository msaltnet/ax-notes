# AX Notes editorial design

Approved visual direction: 2026-10-10 (UTC), implemented in the existing Android 0.3.0 candidate. AX Notes is a personal technical journal for reading experiments and development records, linking thoughts to articles and revisiting them. It is not a book-tracking app.

## Direction and references

The approved concept, `AXNotes-editorial-concept.png`, was inspected before implementation. Its defining choices are warm paper and dark ink, expressive Korean serif headings, quiet sans-serif controls and body text, thin rules, margin annotations and a tablet list/reader composition.

Public references informed the direction:

- [Obra / shadcn UI Community resource](https://www.figma.com/community/file/1514746685758799870): compact controls, small corners and a restrained outline vocabulary.
- [Untitled UI Free](https://www.untitledui.com/free-figma-ui-kit): title/metadata/content hierarchy and spacing. Its free and paid offerings are distinct; no paid capability or asset is used.
- [Radix Themes](https://www.radix-ui.com/themes/docs/overview/getting-started): restrained component and state treatment.

These are inspiration references, not imported libraries or copied templates. The Figma Community resource was not editable from this workspace. No Figma file/library was created or connected. No restricted kit artwork, icons, design variables or code was copied. AX owns this composition and token mapping.

The earlier Material 3 visual direction is superseded. Existing Compose Material 3 controls remain the implementation foundation for focus, press, disabled, selection and accessibility behavior; using them does not require Material's default card-heavy appearance. This avoids replacing working Android behavior for a visual change.

## Production source of truth

- `AxDesignSystem.kt`: light/dark roles, typography, spacing, shapes and dimensions.
- `AxComponents.kt`: ruled/selectable content rows, section tabs, actions, forms and notices.
- `NotesScreen.kt`: journal heading, search shortcut, classification and numbered article index.
- `AxApp.kt`: masthead, compact navigation/rail, saved records and reader margin actions.
- `AxReaderStyle.kt` and `ReaderWebView.kt`: HTML and native fallback reading treatment.
- `AxDesignSystemCatalog.kt`: development catalogue of the same production components.
- [Generated token handoff](../design/ax-tokens.json): exported from Kotlin, not a separately maintained palette or verified Figma import format.

Regenerate with `python scripts/export-design-tokens.py`; verify with `python scripts/export-design-tokens.py --check`. Unsupported token expressions fail explicitly. Editorial font-resource changes must be reflected in the exporter rather than silently inheriting stale font metadata.

## Paper and ink

| Role | Light | Dark |
| --- | --- | --- |
| Canvas / reader | `#F8F7F0` | `#20271F` |
| Main ink | `#293026` | `#E6E6D8` |
| Supporting text | `#515C52` | `#C3CCC0` |
| Action ink | `#40563B` | `#D1DCB7` |
| Selected row | `#E9EDDF` | `#3B5141` |
| Selected foreground | `#2E3B28` | `#E9EDDF` |
| Thin rule | `#CDD3C7` | `#424D43` |

Unselected content rows use the page surface, with no elevation or enclosing rounded border. Selection adds both a tint and a 3dp leading rule with selected semantics. Important state is not conveyed through color alone. Warnings/errors retain paired accessible foreground/background roles and explicit text. The remaining complete role definitions live in Kotlin and the generated JSON. Wallpaper-derived dynamic colors are intentionally disabled.

## Typography and bundled font

| Use | Family | Size / leading |
| --- | --- | --- |
| Journal heading | Nanum Myeongjo Regular | 30sp / 40sp |
| Screen title | Nanum Myeongjo Regular | 24sp / 34sp |
| Article index title | System sans, semibold | 16sp / 24sp |
| Body / controls | System sans | 16/24, 14/20, 12/16 |
| Reader title | Nanum Myeongjo Regular | 32 / 44 |
| Reader body | System sans | 18 / 30 |
| Reader h2 | Nanum Myeongjo Regular | 24 / 34 |
| Reader pullquote | Nanum Myeongjo Regular | 22 / 34 |

Compose uses scalable `sp`; WebView type uses CSS pixels with unitless leading and system text zoom. Layout uses `dp` and CSS pixels respectively. Article rows use compact sans titles like the concept; the journal and reading headlines supply its serif identity. No font-size shrink-to-fit is used. Bounded list excerpts still lead to full article content.

Korean serif appearance must not depend on a device's generic serif fallback. The app bundles the **unmodified Nanum Myeongjo Regular** font from the [official Google Fonts repository](https://github.com/google/fonts/tree/main/ofl/nanummyeongjo), copyright 2010 NHN Corporation, under SIL Open Font License 1.1. The original font contains all 11,172 modern Hangul syllables. No glyph subset, outline modification or renamed font family was produced.

- File: `app/src/main/res/font/nanum_myeongjo_regular.ttf`
- SHA-256: `7ed9e8653a8ed04285d51dc343ffea6eb3d9c73afc27383ea8929ee4ffd03205`
- Original copyright and license: [packaged OFL text](../app/src/main/assets/licenses/NanumMyeongjo-OFL.txt)
- Users can read the full license offline in Settings → 글꼴 라이선스.

Compose and the native fallback resolve that resource directly. The WebView serves the same bytes only for an exact synthetic HTTPS font URL. That URL never reaches the network. CSP permits only that font endpoint; unknown paths, query variants and non-GET requests on the synthetic host are blocked. File/content access, JavaScript and mixed content remain disabled. Public HTTPS article images retain the existing network behavior. No extra font-service request, dependency or user-data transmission is introduced.

## Rhythm, controls and navigation

- Page margin 24dp; spacing scale 4, 8, 12, 16, 20, 24, 32dp.
- Article rows have 16dp vertical space, a small ordinal gutter, title, excerpt and factual publisher/date/cache/bookmark metadata.
- Rows have square corners. Buttons/inputs/filters use 4dp, dialogs 8dp. No content shadow or repeated filled-card grid.
- Thin section rules and 48dp-minimum underlined tabs replace large classification pills. Both Notes and Library expose a selectable group.
- Phone navigation is still a 56dp icon-only content bar plus system inset. Notes, Library, Search and Settings now share it; each has a spoken label, tab role and 48dp-minimum target. Selection is a small ink dot. Search also has a useful journal-page shortcut.
- Tablet rail is 72dp, or 88dp at 150%+ text, with a masthead and named icon destinations. It remains scrollable in short windows.
- Save, loading, disabled, destructive, warning and input-error states keep native Material behavior and explicit messages. A visually quiet control is still a full-size touch target.

## Reader and margin notes

The header shows a real publisher-provided collection title when available, otherwise AX NOTES, followed by title and publication date. Headings, pullquotes and metadata rules establish the long-form rhythm. Code blocks and tables scroll horizontally; images fit the reading column. No arbitrary reading time, completion percentage, streak or “continue reading” claim is fabricated from the conceptual illustration.

Bookmark, memo, reminder and share actions remain available. The lower reader margin exposes cached-body information, the actual linked-memo count and a direct “메모 남기기” action. Direct entry uses the existing editor, save return and dirty-draft protection. Cached body status is not a bookmark or completed-reading state.

The outer reader is capped at 880dp. HTML is centered in a 764 CSS-pixel border-box column with responsive 18–32px horizontal padding; native fallback matches the column policy. Body/title colors update when an existing view changes theme. Native text remains selectable, and fallback title styling responds to font scale.

## Preserved product contracts

- Real feed remains the default; historical sample preference migration, refresh throttling and accessibility refresh are unchanged.
- Series/projects use publisher-authored IDs, names and order. Missing metadata does not infer groups from titles.
- Bookmarks, linked memos, reading reminders and downloaded bodies remain distinct.
- Content/personal database schemas, migrations, package ID and unpublished version `0.3.0` / code `3` are unchanged by this visual follow-up.
- Below 600dp: compact navigation. At 600dp: rail. At 840dp with height ≥480dp: list/detail; at 150%+ text the split threshold remains 1000dp. Existing draft, list and reader-position protections remain. See [adaptive design](ADAPTIVE_DESIGN.md).
- No book model, reading-progress persistence, account, sync, Figma integration, merge, deployment or installation on a user's device is added.

## Verification

Tests cover semantic contrast, explicit type families and sizes, shapes, token drift, component selection/action states, 200% Korean layout, navigation/collections, direct margin-note save/discard behavior, reader metadata escaping, font request boundaries, fallback scaling/theme updates and existing data/adaptive flows.

Run `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin`, the token drift check and the Python export tests. Exact results and screenshot coverage belong in [RELEASE_0.3.0.md](RELEASE_0.3.0.md). A compiled preview or instrumentation APK is not evidence of device execution; software-emulator evidence is not a physical-device/TalkBack certification.
