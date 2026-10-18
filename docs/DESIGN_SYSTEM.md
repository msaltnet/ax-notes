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
- `NotesScreen.kt`: journal heading, classification and numbered article index. Search is reached through the persistent navigation.
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

## Typography and bundled website fonts

| Use | Family | Size / leading |
| --- | --- | --- |
| Journal heading | Noto Serif KR 600 | 30sp / 40sp |
| Root-page title | Noto Serif KR 600 | 30sp / 40sp |
| Detail title | Noto Serif KR 600 | 24sp / 34sp |
| Article index / section title | IBM Plex Sans KR 600 | 18sp / 28sp |
| Input / primary body | IBM Plex Sans KR | 17sp / 26sp |
| List excerpt / controls | IBM Plex Sans KR | 16sp / 24sp |
| Supporting metadata | IBM Plex Sans KR | 14sp / 20sp |
| Reader title | Noto Serif KR 600 | 32 / 44 |
| Reader body | Pretendard Variable | 18 / 30 |
| Reader h2 | Noto Serif KR 600 | 24 / 34 |
| Reader pullquote | Noto Serif KR 600 | 22 / 34 |

Compose uses scalable `sp`; WebView type uses CSS pixels with unitless leading and system text zoom. Layout uses `dp` and CSS pixels respectively. Article rows use compact sans titles like the concept; the journal and reading headlines supply its serif identity. No font-size shrink-to-fit is used. Bounded list excerpts still lead to full article content.

The owner requested the same font roles, copy and AX favicon style as [ax.msalt.net](https://ax.msalt.net/). Headings use the original official regional **Noto Serif KR SemiBold** resource, controls use **IBM Plex Sans KR 400/500/600**, and article prose uses **Pretendard Variable**. The app retains its larger 18sp reader body, comfortable native tracking, approved paper/ink palette, and scalable controls rather than copying desktop CSS dimensions.

All fonts are bundled offline, unmodified, with original OFL notices and copyright metadata. The official Noto Korean regional subset is 7.6 MB versus the 23.8 MB full variable file; it preserves all modern Hangul syllables and extensive Jamo/Hanja. No custom text-only subset was made. Every bundled font covers all 11,172 modern Hangul syllables; system fallback remains available for unsupported Hanja, archaic/decomposed Jamo and emoji. Sources, hashes and exact coverage are documented in [FONT_ASSETS.md](FONT_ASSETS.md). Settings → 글꼴 라이선스 exposes all three notices offline.

Compose and the native fallback resolve resources directly. WebView serves exactly three packaged font GET subresources (editorial OTF, body TTF, metadata TTF); those URLs never reach the network. CSP permits only those explicit URLs. Unknown paths, query/fragment variants, main-frame requests and non-GET requests on the synthetic host remain blocked. File/content access, JavaScript and mixed content remain disabled. No font-service request, dependency or user-data transmission is introduced.

The shared masthead and compact rail use the same overlapping serif AX path as the website's favicon, with a single accessible “AX Notes” label. Home uses the website heading “일이 달라지는 순간을 기록합니다.” and short description “악소리 나는 AX 경험담.”. The launcher separates a full-bleed #191915 background and cream #f3f0e7 mark, and provides glyph-only Android 13 monochrome geometry. Its centered foreground fits inside the 66dp circular safe zone of the 108dp adaptive canvas. Launcher masks are owned by Android; no mask or shadow is baked into the foreground.

## Rhythm, controls and navigation

- Page margin 24dp; spacing scale 4, 8, 12, 16, 20, 24, 32dp.
- Article rows have 16dp vertical space, a small ordinal gutter, title, excerpt and factual publisher/date/cache/bookmark metadata.
- Rows have square corners. Buttons/inputs/filters use 4dp, dialogs 8dp. No content shadow or repeated filled-card grid.
- Thin section rules and 48dp-minimum underlined tabs replace large classification pills. Both Notes and Library expose a selectable group.
- Phone navigation is still a 56dp icon-only content bar plus system inset. Notes, Library, Search and Settings now share it; each has a spoken label, tab role and 48dp-minimum target. Selection changes only the icon to the action color; unselected icons use the supporting foreground on the same paper surface. Press, keyboard-focus and hover feedback share a centered, clipped 48dp circle while the full destination remains tappable. The selected semantic state remains available to assistive technology. The same bar remains visible on Search and Settings. The redundant journal-page search shortcut is removed; Search is reached from navigation.
- Tablet rail is 72dp, or 88dp at 150%+ text, with a masthead and named icon destinations. It remains scrollable in short windows.
- Save, loading, disabled, destructive, warning and input-error states keep native Material behavior and explicit messages. A visually quiet control is still a full-size touch target.

## Reader and margin notes

The header shows a real publisher-provided collection title when available, otherwise AX NOTES, followed by title and publication date. Headings, pullquotes and metadata rules establish the long-form rhythm. Code blocks and tables scroll horizontally; images fit the reading column. No arbitrary reading time, completion percentage, streak or “continue reading” claim is fabricated from the conceptual illustration.

Bookmark, reminder, memo and share actions remain available in that order above the article. The reader has one memo action immediately after the reminder; the lower memo area is removed. “메모 남기기” opens a body-only editor when no memo exists, and “메모 수정” opens the newest existing record. New saves permit one memo per article. If earlier records coexist, the target is chosen by descending edit time, then descending creation time, then ascending ID, so equal timestamps do not change the selected record unpredictably. Other stored records remain accessible as ordinary Library/Search cards. Memo titles have no editor, read-only label or card presentation; every memo card previews its first nonblank body line. Stored title bytes and record IDs are left untouched, with no database migration or cleanup.

When the action row overflows, a fading edge and a 48dp named arrow control show that more actions are available. Paging and ordinary horizontal swipes expose Share without shrinking type. The fixed trailing control pages forward, then becomes a “처음 작업 보기” arrow at the end. Its separate gutter never covers action text or intercepts drags inside the row. Non-overflowing rows have neither extra control. Direction follows the layout direction and system text scale is preserved.

Opening a cached article uses its already-observed body immediately, rather than first showing a false missing-content warning. Unknown deep-linked records show a neutral loading state until their first database result. The current reader stays attached, visually hidden, excluded from accessibility and blocked from focus while its memo editor is open. Cancel, discard and save return to the same rendered document and scroll position within the same adaptive layout. Activity recreation or crossing the one-/two-pane breakpoint recreates the native view using saved reader state. Leaving the reader flow, changing articles or destroying its activity releases the native view; this is not a cross-article WebView cache. Changes to non-rendered cache metadata do not reload identical HTML; real content/URL/theme changes still update it, and font scale updates native text zoom.

Direct memo entry retains save return and dirty-draft protection. The article-list cache icon describes offline availability, independently of bookmarks. Settings → 오프라인 읽기 안내 explains automatic caching and uncached title/summary-only entries. A rendered reader body has already been cached; no extra saved-state badge is needed.

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

## PR 8 device-feedback refinement (2026-10-11 UTC)

Owner feedback: https://github.com/msaltnet/ax-notes/pull/8#issuecomment-6105732055.

- `AxMasthead` is reused across the journal, reader and root destinations. Library, Search and Settings share the same `AxPageHeading` title/description rhythm. Root navigation is always available; detail screens retain Back and draft protection.
- Settings uses separate, spaced sections with 18sp semibold headings, 16sp body text, 14sp supporting text, and distinct outlined actions. The duplicate AX Notes title was removed. Data deletion confirmations and all warnings remain intact.
- `AxReminderDialog` stays in the app's light/dark theme. Full-width date/time selectors show their selected values, followed by an explicit combined summary. Filled 예약 and outlined 취소 actions are distinct. Applying a date/time only updates the draft; final reservation keeps the existing notification-permission and scheduler flow.
- Date selection converts Material's UTC calendar date back into the user's local date without changing the chosen local clock time. Time selection uses simultaneous 24-hour and minute scroll wheels, center snapping and accessible adjustment actions. Both wheels retain full scalable typography; no numeric keyboard fields are exposed. Dismiss/Cancel does not apply a picker draft. Past-time validation remains inside the dialog.
- Database schemas, package/version, personal records, notification behavior, external links and release scope are unchanged by this refinement.

### Calendar and scroll-wheel reminder pickers

The [second device-feedback round](https://github.com/msaltnet/ax-notes/pull/8#issuecomment-6108678891) replaces the earlier numeric-entry modes. Date selection always shows the calendar with no input-mode toggle and extra lower action spacing; time uses side-by-side hour/minute wheels. Both dialogs use non-floating inset-aware windows. DatePickerDialog also explicitly fills its outer window before Material applies its internal wrap-content centering; this is necessary for real API-26 IME insets even when adjustResize is already requested. System bars reduce the available content area; the body can scroll while Cancel/Apply remain reachable. The outer date-window policy is retained from the earlier API 26 inset fix, although the current pickers no longer open a keyboard. The [DialogProperties contract](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/DialogProperties) sets resize behavior on API 30 and below and inset handling on newer versions.
