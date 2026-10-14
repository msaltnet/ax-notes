# Adaptive Android reading workspace

Implemented 2026-10-08. This change keeps the existing local bookmarks, linked memos, search and delayed reading reminders; it changes their layout and navigation rather than the data contract.

## Layout decisions

The available Compose window bounds determine the layout, not the device model or an orientation lock. Resizing a tablet into a narrow split-screen window uses the same compact experience as a phone.

- Below 600dp: single pane, existing Notes/Library bottom navigation and Search/Settings app-bar actions.
- From 600dp: persistent, scrollable side navigation for Notes, Library, Search and Settings. One content pane remains below the two-pane threshold.
- From 840dp, with at least 480dp window height: list and detail side by side. Notes, Library and Search keep their source list visible while opening an article or editing a linked memo. Settings remains a centered single page.
- At 150% or larger text: two panes start at 1000dp. The editorial icon rail is 88dp instead of 72dp, and the list gets 380dp. At normal text the list grows from 320dp to a maximum of 400dp.
- Very short landscape windows keep a single pane even if they are wide. The navigation rail scrolls rather than clipping destinations.
- No article is auto-selected. The initial detail pane explains how to begin; Back from a selected article returns to that state while preserving the list position.

The 600dp/840dp breakpoints follow [Android window-size guidance](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes). The list/detail behavior follows the [canonical list-detail pattern](https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail). The app uses its existing Compose dependencies and a small testable layout policy rather than adding another navigation framework.

## Reading, editing and accessibility

- Reader HTML has a centered 764 CSS-pixel outer column with responsive padding, leaving about 700px of text at its widest. Compose reader space is capped at 880dp; other single-pane pages are capped at 760dp.
- Code blocks and tables still scroll horizontally, images fit the column, and text scales with system font size. JavaScript, file/content access and insecure content remain disabled.
- The reader tracks relative scroll progress for restoration after recreation/reflow. This is an approximate visual position, not a DOM paragraph anchor; late image loading can shift content.
- Lists retain explicit saveable scroll states. The selected article, library filter, editor draft and pending discard destination remain saveable across recreation.
- Persistent rail navigation and selecting another list item share the existing dirty-memo confirmation. Cancel/continue keeps the draft; discard performs the requested destination once. Navigation is blocked while a save is in progress.
- The entire memo form scrolls with IME insets, keeping Save reachable in short windows and large text. Search guidance and results also share a scrollable list. Reminder dialog content scrolls.
- Selected article cards expose selected semantics and a visible color. Material interactive components provide minimum touch areas, and custom saved-item click targets explicitly have at least 48dp height.

## Verification scope

Added breakpoint unit checks and Robolectric Compose flows for compact, medium and expanded windows, list/detail Back behavior, dirty-editor rail/list cancellation, state recreation, live window resizing, and 200% text scrolling. Existing UI tests explicitly enable the bundled snapshot as a deterministic fixture; the device suite uses the app's default live feed with a longer initial wait.

Execution results belong in the release's current test report. Adding a test is not evidence that it passed. Real WebView rendering, tablet screenshots, keyboard behavior, TalkBack traversal, fold hinges and physical-device performance need device/emulator execution. The layout is width-aware but does not inspect a physical folding hinge, and no physical-device certification is claimed.

### Executed automated checks · 2026-10-08

The final `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` aggregate completed successfully after local SDK/JDK, trust-store and writable test-home setup. Test XML was inspected: 69 tests passed, with zero failures, errors or skips. This includes five layout-policy tests, nine adaptive UI tests and the three existing UI tests. Both app and instrumentation APKs assembled.

The nine adaptive flows cover phone navigation, medium-width single-pane rail navigation, expanded list/detail selection and Back, dirty-editor rail/list cancellation, draft/discard restoration after recreation, live window-width changes, 200% text with reachable controls, bookmark/memo/search/reminder-cancel continuity including Library/Search Back, and accurate cached-source labels after the desired connection changes. These are Robolectric Compose checks, not real WebView or physical-device execution.

Lint reported zero errors and 13 nonblocking warnings: three dependency-update suggestions, eight KTX-style suggestions and two localization/string-composition suggestions in the existing plaintext fallback. Device/emulator evidence is recorded separately by the release owner and must identify the APK actually exercised.
