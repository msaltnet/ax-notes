# AX Notes 0.3.0 feedback candidate

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
