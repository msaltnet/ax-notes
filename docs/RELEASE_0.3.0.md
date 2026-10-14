# AX Notes 0.3.0 feedback candidate

Current candidate: the 2026-10-11 PR 8 owner-feedback and website-brand follow-up. Its final automated result is 143 tests / 19 suites, 12 token checks and zero lint errors; the final section records its distinct APK and runtime scope. Earlier candidate counts, screenshots and installation notes below are historical, not inherited verification.

Based on merged main `6be0aedc0625e2b5c3679e0cbc4459c623bcdbfc`. Implements [feedback #6](https://github.com/msaltnet/ax-notes/issues/6); not a published release.

## Changes

- The approved editorial direction uses paper/ink, Korean serif headings, compact sans article rows and thin rules. Shared light/dark colors, typography, shapes and spacing live in `AxDesignSystem.kt`. See [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) for references, implementation and font licensing. Material controls remain the behavior foundation.
- AX monogram on the notebook launcher icon; distinct archive-box library icon.
- Icon-only 56dp compact navigation plus system inset, with accessible names and tab/selected semantics. Existing tablet rail and list/detail behavior is retained.
- Pull-to-refresh and equivalent accessibility action replace the visible refresh button. Repeated requests are ignored while refreshing.
- Live web data is the user-facing source. Historical sample preferences migrate once without deleting cached articles or personal records; programmatic fixtures are debug-only.
- Separate series/project tabs use publisher-authored IDs, titles and order, including article counts and drill-down. Titles never infer membership.
- Reader actions are bookmark, memo, reading reminder and share, with distinct icons and short labels. Author, original-page and project buttons are removed from that toolbar.
- Saved text is indicated by a body-cache icon and optional explanation. Downloaded text remains readable offline; images and external links may need the network. Bookmarks, memos and reminders are separate records.
- Version `0.3.0`, versionCode `3`; debug application ID remains `net.msalt.axnotes.internal`.

## Data compatibility and rollout

Content DB v1 → v2 adds six nullable taxonomy columns in place. Personal DB remains v1. No destructive migration or package rename is used. Migration tests start from the exact exported v1 schema and preserve cached text, feed state, unavailable article rows and personal snapshots.

The companion `ax-notes-public` source change adds optional schema-v1 taxonomy fields. PR #3 merged on 2026-10-10; an HTTPS fetch of the public manifest after the merge returned nine notes with publisher-authored series/project fields. Older manifests remain readable with no invented groups. The empty group state says metadata has not arrived rather than asserting the website has no groups.

## Initial issue-6 verification

The first issue-6 commit (`98ac1817`) passed the checks below on 2026-10-10 (UTC). The later design-system follow-up is verified separately below; these results alone do not cover it.

- Web verify, default subpath and production root: 50 tests passed on each path after reconstruction.
- Web real-pipeline draft exclusion: passed.
- Design: 24 checked color pairs exceeded 4.5:1 (minimum 5.98:1); vector XML and visual previews passed.
- Android aggregate `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`: passed using Gradle 8.11.1, JDK 21 and Android SDK 35.
- All 89 JVM/Robolectric tests passed (13 suites; no failures, errors or skips), including 10 issue-6 UI regressions, tablet flows, schema migration and root/subpath feed compatibility.
- Android lint: 0 errors, 10 advisory warnings (KTX suggestions and existing TextView translation warnings).
- Debug app and instrumentation-test APKs assembled successfully. App metadata verified as `net.msalt.axnotes.internal`, version `0.3.0`, versionCode `3`. Instrumentation tests were compiled, not run on a device.
- Independent source review found no blocking issue. An independent SQLite comparison verified that the actual migration matches exported schema 2 and preserves existing fields.
- Companion web PR: [ax-notes-public #3](https://github.com/msaltnet/ax-notes-public/pull/3). Its exact-head hosted build passed; deployment was skipped.
- This Android repository has no hosted CI workflow; local checks do not imply a hosted CI pass.
- Android device/emulator UI, real WebView images, TalkBack and physical-device QA: not verified for this version.
- Figma account library installation/synchronization: not performed; official kit referenced and existing Compose library configured.

## Earlier design-system follow-up (`f2221fa`)

The selected official Material 3 kit now informs actual shared component contracts, not only theme colors:

- Reusable cards, primary/outlined/text/destructive actions, filters, input fields, notices and empty states are used by the app.
- Paired light/dark roles, the full typography scale, spacing, shapes and component dimensions are defined centrally. No alternate UI framework or font dependency was added.
- Long-form reader CSS and native fallback share semantic colors and reader type/layout tokens, with unitless CSS line heights for font scaling.
- `AxDesignSystemCatalog` renders the same components in their real states. [Token handoff](../design/README.md) includes a generated JSON export and a drift check.
- Existing tablet navigation, stored data, classification semantics, reminders and draft protections are retained. Version stays 0.3.0/code 3 because this extends the same unpublished candidate.

Earlier follow-up verification on 2026-10-10 (UTC); this is not the editorial candidate’s result:

- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: passed.
- All 112 JVM/Robolectric tests passed (16 suites; failures/errors/skips 0). The 23 new checks include 60 semantic text-color pairs across light/dark, real component states, native-graphics Korean layout at 200%, and reader/fallback tokens.
- Lint: 0 errors, 5 KTX recommendation warnings. The legacy WebView database-disable call still emits a compiler deprecation warning; it remains explicitly disabled for older supported Android versions.
- Debug app and instrumentation-test APKs built; release Kotlin also compiled without the debug-only preview dependencies. Package/version readback remains `net.msalt.axnotes.internal` / `0.3.0` / code `3`.
- Token export drift check and 10 Python export/mutation tests passed. They verify that typography changes are exported and unsupported token expressions fail rather than silently disappearing.
- Independent review found and resolved long reminder-state truncation and incomplete token drift detection. Existing data, tablet and memo flows passed the integrated regressions.
- The four Compose preview entry points compile; Android Studio preview rendering itself was not run. Robolectric PNG capture timed out waiting for a window redraw, so no screenshots are claimed. Native text-layout checks passed; they are not a substitute for device visual QA.
- Actual Figma file/library creation, device/emulator instrumentation execution, TalkBack and installed-app update compatibility remain unverified. No merge, deployment, release or device installation was performed.

## Approved editorial follow-up

The 2026-10-10 approved visual concept supersedes the earlier Material visual direction while retaining Compose control behavior. It adds paper/ink light and dark themes, bundled Korean serif headlines, a compact ruled article index, underlined section tabs, a four-destination 56dp icon bar and reader margin actions with actual linked-memo counts. No conceptual reading progress is fabricated. The unmodified SIL OFL font and full offline license are bundled; native and HTML readers use the same bytes without a network font service.

Database schemas, package and 0.3.0/code 3 are unchanged. The companion web PR #3 has since merged; live taxonomy availability is separate from the Android build. This change makes no web edits.

Final editorial code verification on 2026-10-10 (UTC):

- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: passed using Gradle 8.11.1, Temurin JDK 17.0.20.1 and Android SDK 35.
- **122 JVM/Robolectric tests passed**, 16 suites, zero failures/errors/skips. This includes 10 additional editorial regressions and all earlier feed, migration, bookmark, memo, reminder and tablet flows.
- **11 Python token export/mutation tests passed**; generated-token drift check passed. The staged whitespace check passes for authored files; the verbatim upstream OFL license is excluded because its original text contains trailing spaces.
- **Lint: 0 errors, 9 warnings**. Five are existing KTX suggestions; four recommend newer versions of unchanged dependencies. Dependency upgrades were intentionally left outside this visual change. The known legacy WebView database-disable compiler deprecation remains.
- All 60 light/dark text-color pairs exceed 4.5:1; minimum ratios are 5.08:1 light and 5.68:1 dark. Native 200% Korean layout, true Android font-scale masthead growth and full touch-target checks pass.
- Existing small-window UI tests now wait for loaded data and scroll to lazy article rows instead of assuming the first article is above the fold. Their paused-looper synchronization and per-test Application-factory isolation are explicit. A masthead check measures rendered glyph bounds rather than mistaking a retained paragraph constraint for actual overflow.
- Reader tests cover escaped metadata, exact packaged font bytes, blocked synthetic-host URL/method variants, offline native title font, scalable title leading, unchanged body leading and selection preservation. A narrowly scoped `ResourceType` suppression documents Android's valid raw-byte access to a packaged `R.font` resource; no lint baseline or broad suppression was added.
- Debug app and instrumentation APKs assembled; release Kotlin compiled. App remains `net.msalt.axnotes.internal`, version `0.3.0`, code `3`, minimum API 26, target 35. The debug APK is 12,684,205 bytes, SHA-256 `edd2ba81caac1bb57bf3e60072fe5cd7c6487df8f6529be4dbbfa2d18227be01`; installed identity is recorded with emulator evidence.
- Source/control review found no blocking issue. The repository still has no hosted Android workflow; these are executed cloud-workspace checks, not hosted-CI results.

### Actual app visual/runtime evidence

The final debug APK was installed on a disposable API 26 x86 software emulator and its installed `base.apk` pulled back. Its SHA-256 exactly matches the built/reviewed APK above. A real Chrome `69.0.3497.100` WebView renders the cached article and bundled Korean serif title/subheading.

Unedited [app screenshots and provenance](editorial-evidence/README.md) cover light phone Notes, dark phone reader, 200% system text and a 1280 × 800dp tablet list/reader split. Independent pixel review found no blocking layout issue in these captures. Software-emulator startup initially showed a System UI ANR and blank WebView frame; only settled, overlay-free renders are included. This environment is not representative performance evidence.

The live-feed instrumentation attempt did **not** pass: emulator DNS returned `EAI_NODATA` for `ax.msalt.net` before importing articles or creating personal test records. The host successfully fetched the current public HTTPS manifest and all nine details; that exact snapshot was imported into the emulator's content cache for visual QA. No personal records were imported or fabricated. These are actual cached-content app renders, not proof of live emulator networking. No production network/security setting was changed to work around the environment.

Physical devices, TalkBack, remote WebView images, current WebView versions and production-signed upgrade retention remain unverified. Compilation of the instrumentation APK is not an instrumentation-suite pass.

## Installation safety

No release or installation on a user's device is included. The disposable cloud emulator is only a verification target. The supplied debug artifact is signed with this workspace's Android debug certificate; compatibility with an existing installation's signing key has not been established. Any in-place update requires the same signing certificate as the installed app. Version-name cleanup does not change that requirement. Do not uninstall the existing app, clear data or publish a differently signed replacement to work around an update failure.

## PR 8 owner-feedback refinement — 2026-10-11 UTC

This follow-up starts from `60ab79ba348afe1c885fe158283d1500e67afe70`, preserving its adaptive-test application-factory isolation fix. It addresses the [owner's eight review comments](https://github.com/msaltnet/ax-notes/pull/8#issuecomment-6105732055).

- Reminder date/time selectors show the chosen values and a combined confirmation summary. Filled 예약 and outlined 취소 buttons distinguish the actions. Date and numeric-time pickers stay in the app theme, use staged edits, and return to the previous summary scroll position.
- Date/time conversions preserve local calendar dates and wall-clock time; invalid numeric values and past reservations are rejected. Existing notification permission and scheduling behavior remain in place.
- The always-present reader cache badge and duplicate upper memo button are removed. Offline cache availability remains meaningful in the article list; Settings explains automatic caching and title/summary-only entries. Reader memo count and direct entry are grouped in one lower area.
- Home and reader use one masthead. Library, Search and Settings share a title/description component and persistent root navigation. Selected navigation uses a filled contrasting tile.
- List/section titles increase to 18sp, excerpts/body copy and controls to 16sp, supporting copy to 14sp, and input text to 17sp. Reader body remains 18sp. Settings now has separated headings, body copy, actions and consistent vertical spacing without a duplicate app title.
- No database schema, package, version or personal-data change is introduced.

### Follow-up installation safety

The restored cloud build environment has a newly generated debug signing certificate. Its APK is an internal verification artifact and is not distributed as an update to the previously installed physical-device build. Use the existing local build environment and its original signing key for a compatible, data-preserving update. Do not uninstall or clear data to bypass signature mismatch.

The earlier physical-device note in PR 8 applies to the previous `60ab79b` build. This follow-up has not been installed on the user's phone, merged, deployed or released.

### Intermediate verification before website-brand alignment

Historical evidence for the eight-comment refinement, before the later website-font/icon and IME changes. On that intermediate production source, `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin` passed on Temurin JDK 17.0.20.1 / Gradle 8.11.1 / SDK 35.

- JVM/Robolectric: 135 tests across 18 suites; zero failures, errors or skips.
- New reminder coverage: 7 UI regressions and 5 local-date/time helper tests. Both 360dp and 411dp windows use the actual 200% Android text configuration; tests check full glyph bounds, exact scalable typography, selection updates, cancel/reopen, invalid input, past-time recovery and scroll restoration.
- Token export/mutation: 11 tests pass; generated token drift and whitespace checks pass.
- Lint: zero errors, 9 advisory warnings (5 existing KTX suggestions, 4 unchanged dependency update notices).
- Debug app and instrumentation APKs build; release Kotlin compiles. Building instrumentation is not an instrumentation-suite execution claim.
- Internal APK: `net.msalt.axnotes.internal`, version `0.3.0`, code `3`, minSdk 26, targetSdk 35. SHA-256: `4da0461bd49e62b69c58f583b5eaef75877ef3d91568871512401a86f4b0d1c1`.
- Independent source review found no blocking issue. The repository still has no hosted Android workflow; these are cloud-workspace results.

## Website-brand alignment and final follow-up verification — 2026-10-11 UTC

This section supersedes the intermediate 135-test/hash result above. It includes the owner's subsequent request to align app typography, copy and icon with ax.msalt.net, plus the real-keyboard issue found during verification.

- Headings use original Noto Serif KR 600; UI uses IBM Plex Sans KR 400/500/600; reader prose uses Pretendard Variable. All are bundled offline with original notices. The official Noto regional file preserves all modern Hangul without an app-generated glyph subset. See [font/icon provenance](FONT_ASSETS.md).
- The shared masthead and adaptive/monochrome launcher use the existing site's overlapping serif AX geometry. Website copy is reused on the journal and About section; the approved app palette, mobile hierarchy, larger type and existing behavior remain.
- Both reminder pickers now account for keyboard and system-bar insets. The date host also fills its outer window before Material wraps/centers the picker Surface; this avoids the API-26 WRAP-height inset failure. This resolves the intermediate API 26 observation where 200% text pushed actions beneath the numeric keyboard. The production window-policy regression waits for the actual displayed picker rather than reading its dismissed predecessor.
- Offline license UI identifies every font family and preserves all original notices, including Noto's embedded Adobe copyright attribution.

Final automated rerun after the date-window-height fix, 2026-10-11 07:06–07:08 UTC:

- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: **passed** on Temurin 17.0.20.1, Gradle 8.11.1 and SDK 35.
- **143 JVM/Robolectric tests, 19 suites, zero failures/errors/skips.** Six brand-resource tests cover font coverage/weights/notices, adaptive geometry/raster equivalence and API 33 monochrome. Reader tests verify all three exact font endpoints and fallback typography; reminder tests cover actual 200% scaling and API 26 window policy.
- **12 Python token export/mutation tests** and generated-token drift check pass. Authored-file whitespace check passes; upstream license text remains verbatim.
- **Lint: zero errors, 11 advisories.** Nine existing suggestions remain (5 KTX, 4 unchanged dependency versions). Two new resource advisories concern the redundant API-26 qualifier at minSdk 26 and the base API-26 adaptive XML lacking monochrome; the API-33 override supplies the same monochrome icon for both icon and roundIcon and is covered by the API-33 resource test. No lint baseline or broad suppression was added.
- Debug and instrumentation APKs build; release Kotlin compiles. Instrumentation assembly is not an instrumentation-suite pass.
- Internal debug APK: **24,463,735 bytes**, SHA-256 `df867b96d360ed4fe58f9b7e7a1608d8b0148d65501a5c4dc6b03be90bcb54c5`. Package `net.msalt.axnotes.internal`, 0.3.0/code 3, minSdk 26, targetSdk 35. Its workspace debug certificate SHA-256 is `33a026ceb460d71bc9a5bb37509cd221322360d4a922b5ac1ae7032a499785b7`.
- The APK remains internal because it uses a newly generated workspace key; compatibility with the existing physical-device build's key has not been established. A data-preserving update must be built in the original local environment using its original key; do not uninstall or clear app data.

Independent source/security and selected native-pixel review found no blocker. The separately executed 11-case capture harness is not counted among the 143 regressions. Its Settings light/dark and 200% renders, updated reminder controls, API-28 adaptive masks and API-33 monochrome vector are documented in [current evidence](feedback-evidence/README.md). These native-Skia renders are not device screenshots; real keyboard evidence is identified separately there. No current physical-device, TalkBack, modern-launcher-themed-interaction or signed-upgrade certification is claimed. No hosted Android workflow exists.


Final actual-keyboard gate, 07:21–07:22 UTC: the installed final APK was pulled back and independently confirmed to match the `df867b96…` SHA-256 above. The date picker passes with the real keyboard both shown and hidden at 200% system text, including complete date input and fully visible Cancel/Apply controls. Independent review of the original pixels passed. This complements the preceding candidate's numeric-keyboard/minute-scroll and actual-launcher checks; it is not a claim of physical-device, modern Android or final WebView-prose validation. The exact scopes and privately delivered capture filenames are recorded in [current evidence](feedback-evidence/README.md).


The 07:23 UTC numeric-input smoke check also passed on that exact final APK, with the real keyboard visible at 200%: complete hour/minute labels and values plus both actions remain above the IME. The independent reviewer confirmed this frame at 07:24 UTC. Both picker keyboard checks are thus final-APK results; only the prior launcher and numeric-scroll historical captures retain their earlier build identity.


## Second PR 8 owner-feedback round — 2026-10-11 UTC

This follow-up starts from `84274d806e3ec6a6eaf7bfa3a2a40524b1b0e81b` and addresses the [six new owner requests](https://github.com/msaltnet/ax-notes/pull/8#issuecomment-6108678891). It supersedes the earlier numeric picker and lower reader-memo descriptions above.

- Home removes its duplicate search shortcut; persistent Search navigation remains. Series/project back arrows start at the page gutter without additional button-leading padding.
- Date selection is calendar-only at every text scale, with no edit-to-numeric toggle and additional lower action spacing. Time selection shows hour and minute scroll wheels together, with center snapping, independent selection and accessible adjustment actions. Picker changes remain staged until Apply; Cancel and Back discard the current picker draft.
- The reader has one memo action immediately after the reading reminder and no lower memo controls. A new article memo has body input only; a saved article memo reopens for editing.
- New duplicate memo creation is rejected atomically under the existing shared personal-data mutex, including concurrent repository instances. Retrying the same draft ID remains idempotent.
- Personal DB remains version 1, without migration or cleanup. Existing multiple memos, IDs, stored titles, bodies and creation timestamps survive unchanged on upgrade. Earlier multiple memos remain individually accessible and editable through the reader's single memo action, Library and Search. Existing titles are shown read-only and preserved byte-for-byte when a body is edited. No automatic deletion or merging occurs.
- Untitled cards preview the first nonblank body line; this is display-only and does not create a stored title. Library introduction is exactly “기기에 보관된 북마크와 메모, 알림 입니다.”.
- Package, version 0.3.0/code 3, notification scheduling and permissions are unchanged. No merge, deployment, public APK release, user-device installation, or PR-body rewrite is included.

### Automated verification for the second round

Executed on 2026-10-11 12:13–12:18 UTC with Temurin JDK 17.0.20.1, Gradle 8.11.1 and Android SDK 35:

- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: **passed**.
- **160 JVM/Robolectric tests across 20 suites; zero failures, errors or skips.** New coverage includes concurrent duplicate prevention, stable draft retries, legacy title/record preservation and disk reopening, removed-article Library/Search editing, protected drafts, actual scaled text, native collection-arrow geometry and six-week calendar swipe/selection.
- **12 Python token export/mutation tests**, generated-token drift and authored-file whitespace checks pass.
- **Lint: zero errors, 13 advisories**: 5 existing KTX suggestions, 6 unchanged dependency update notices and the 2 existing adaptive-icon resource advisories. No baseline, new suppression or dependency upgrade was introduced.
- The body-only editor, Calendar-only date picker and simultaneous time wheels compile in both debug and release Kotlin. App and instrumentation APKs assemble; instrumentation assembly is not a device-suite pass.
- Internal debug APK: **23,455,191 bytes**, SHA-256 `429f3f8ac9a5a0c1c7d7568584b680049476f55ae9867416c86b4bca0c7c6ac3`. Package `net.msalt.axnotes.internal`, 0.3.0/code 3, minSdk 26, targetSdk 35. The restored workspace generated a new debug signing certificate (`3ff105d800667e739f6b5483a80d4c503b9a9e9776d52c134e21c048a093428c`); this APK is not distributed as a physical-device update.
- Independent source review found no confirmed production blocker. Independent native-pixel review verified 360×640dp/200% light and dark calendar days, sixth-week selection, year grid and both scroll wheels. These component captures are separate from installed-app execution. Two 100% native captures had repaint artifacts and are excluded from visual approval.
- This repository has no hosted Android workflow; the results above are executed workspace checks, not hosted-CI results.

### Installed-app verification for the second round

The final internal APK was installed on a fresh disposable API 26 Google APIs x86 emulator and pulled back from the package installation. The installed SHA-256 exactly matches `429f3f8ac9a5a0c1c7d7568584b680049476f55ae9867416c86b4bca0c7c6ac3` above. Captures use the eight bundled sample articles, not a live-network success claim.

- At approximately 411dp width / 100% text / light mode, Home has no duplicate upper Search shortcut; the cached article body renders with the bundled fonts and one upper memo action. Real touch swipes changed both hour/minute wheels from 22:40 to 23:42; Apply correctly updated the reminder summary to 11:42 PM. The calendar and year grid have readable labels, no numeric-entry toggle and fully visible, padded footer actions.
- At 360×640dp / 200% system text / dark mode, the calendar remains fully readable and selecting October 12 updates the reminder date correctly. Scrolling the reminder content preserves accessible, fixed Cancel/Schedule actions. Both time wheels and Apply/Cancel fit at full text scale. Real opposing swipes changed 23:42 to 22:44, and Apply correctly produced 10:44 PM without changing the selected date. Independent review of these unedited installed-app pixels found no clipping or selection mismatch.
- The old software-only emulator had System UI ANRs during initial boot and configuration changes, a slow first install/launch and a transient blank initial WebView frame. Only settled frames support the UI results; this is not a startup-performance pass. The system ANR and incomplete frames are excluded from visual approval.
- Actual installed-app interactions complement the complete native/JVM legacy-memo, standalone-record, accessibility-semantics and small-screen regressions; they do not replace physical-device, TalkBack, modern-Android, live-feed networking or device instrumentation-suite verification.

Screenshots remain private rather than adding new image assets to the public repository. No APK is delivered: a compatible update for the existing phone must be built with its original local signing key, without uninstalling or clearing app data. The PR remains a normal unmerged review candidate; its earlier body is not rewritten by this source update.


## Third PR 8 owner-feedback round — 2026-10-11 UTC

This follow-up starts from `dc07fb0791dbff689d75fe90ee1cae217cb44e7e` and addresses the [three new owner requests](https://github.com/msaltnet/ax-notes/pull/8#issuecomment-6110062475). It supersedes the second round's legacy-title and special multiple-memo presentation above.

- Memo titles are absent from the editor and from Library/Search card previews. Every card uses the first nonblank body line. The reader's single action edits the latest memo; newest edit time, newest creation time, then ascending record ID provide a deterministic tie-break. Additional stored records remain ordinary titleless Library/Search entries. No stored record is erased, and the personal DB remains version 1 without migration or cleanup.
- Cached article entry starts from the already-observed article instead of a transient null/missing-content screen. Unknown article IDs wait for their first database result before showing the missing-content state.
- Opening the memo keeps the one current reader attached but hidden and inaccessible. Cancel, discard and save restore the same loaded WebView within the same adaptive layout. Activity recreation or a one-/two-pane layout change creates a new native view with saved reader state. Leaving the flow or changing articles releases it normally; it is not an unbounded view cache. Non-rendered metadata changes no longer reload identical HTML.
- A fading edge and named 48dp paging arrows expose overflow in the reader action row. Horizontal swipes remain supported, full-size text is retained, and a separate arrow gutter pages forward or returns to the start without covering action targets.
- Package, version 0.3.0/code 3, feed contract, notification scheduling and permissions remain unchanged. No merge, deployment, public APK release, user-device installation, new public screenshot asset, or PR-body rewrite is included.

### Verification for the third round

- Controlled JVM reproductions against the unchanged `dc07fb0` production source demonstrated that memo entry destroyed the loaded reader and that a cache-timestamp-only change reloaded identical HTML. The corresponding candidate tests exercise the same paths without the destruction/reload.
- **172 JVM/Robolectric tests across 22 suites; zero failures, errors or skips.** Coverage includes titleless old/new/orphan memo flows and deterministic latest-memo selection; retained-view cancel/discard/save, hidden-reader focus/accessibility, cleanup and recreation; actual touch swipes and paging, Share hit targets, RTL, 200% text and width changes.
- **12 Python token export/mutation tests**, generated-token drift and authored-file whitespace checks pass.
- Independent source review found no confirmed blocker. It identified and verified fixes for hidden-reader keyboard access and overlapping action-row touch targets. Hardware Tab input in the multiline test editor correctly invokes the existing dirty-draft protection; the clean-cancel test restores its empty fixture before checking Cancel.

- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin`: **passed**, 2026-10-11 15:21–15:25 UTC, using Temurin 17.0.20.1, Gradle 8.11.1 and SDK 35.
- **Lint: zero errors, 11 advisories** (5 existing KTX suggestions, 4 dependency-version notices and 2 existing adaptive-icon resource advisories). No suppression, dependency change or baseline was added; update-notice counts depend on the build environment.
- App and instrumentation APKs assemble; release Kotlin compiles. Instrumentation assembly alone is not a device-test result. There is no hosted Android workflow.

- Internal debug APK: **23,752,447 bytes**, SHA-256 `6346b7e1c5d3d784c8fc81d020b523ae158d3d7a0b1d7f9040f5060e1d47a1e9`. Package `net.msalt.axnotes.internal`, 0.3.0/code 3, minSdk 26, targetSdk 35. Its new workspace debug certificate SHA-256 is `3b0b8cdad73f1d010da5857de21a5316a5914a3ddd7e96cc86e1f6d0807830cb`; it is not delivered as a phone update.

### Installed-app check for the third round

A focused instrumentation test passed on the exact final APK above, installed in a disposable read-only overlay of the API 26 Google APIs x86 emulator. Pulling the installed `base.apk` back reproduced the exact `6346b7e1…1d47a1e9` SHA-256. The run uses bundled public sample articles, not live-feed networking.

- The native test waits for WebView's visual-state commit, rather than equating a resource-load progress percentage with a rendered document. A real scrolled article survives memo Cancel and Save with the same WebView instance and exact scroll offset.
- Actual touch paging through the new overflow control reaches the visible Share action and returns to the start. The focused native test passes **1/1**; this is not a full device-instrumentation-suite pass.
- Earlier baseline attempts were obstructed by a system-server watchdog and System UI ANRs. The original AVD and locks were preserved; a supported read-only overlay was used, and an opt-in test-only fixture selected the exact system dialog's Wait action once. No production recovery code, data reset or migration was added.
- After that recovery, the installed unchanged `dc07fb0` baseline rendered its reader but crashed with Compose's `LayoutNode should be attached to an owner` after the memo tap. A clean before/after visual timing comparison was therefore not obtained. The final candidate completed the reader/memo test without another recovery. This does not establish the crash's root cause or certify that every perceived flicker on every device is eliminated.
- Independent pixel review accepts the settled Share/end-arrow and post-save reader frames. The editor/input and early reader captures are transitional and are excluded as evidence of typed input, Save enablement or before/after timing. Screenshots remain private. Physical-device, TalkBack, modern-Android, live-network, native 200%/rotation/IME-layout and original-signing-key upgrade checks remain outside this focused native result. The full JVM suite includes 200% typography, rotation/recreation and RTL regressions.

The later native-harness-only refinement was rebuilt with `assembleDebugAndroidTest lintDebug`; the production APK hash did not change. Independent source review passed. The PR remains an unmerged review candidate, with no APK distribution, deployment, user-device installation or PR-body edit.
