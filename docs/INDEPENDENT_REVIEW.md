# Independent MVP source review

Reviewed 2026-10-07 against PRD, TRD and IMPLEMENTATION_PLAN; final-change reread at 11:17 UTC. Scope: Android app/data/UI/reminder sources, manifest, backup rules, build configuration and current tests. This is a source review with focused checks and inspection of the implementation team's runtime evidence, **not complete app verification**. No remote changes or publication were performed.

## Fixes re-read and verified in source

- **P1 — broken search SQL:** `Databases.kt` now uses two source backslashes in ordinary Kotlin strings, preserving SQLite's single-character backslash ESCAPE. Previously Kotlin removed the slash and SQLite received `ESCAPE ''`.
- **P1 — main-thread parsing:** `ContentRepository.refresh/load` now run JSON parsing and HTML sanitization on `Dispatchers.IO`; cancellation is rethrown.
- **P1/P2 — memo save interruption:** a saveable draft UUID makes retrying a new memo idempotent. Back and incoming navigation wait while saving; cancellation is not reported as a normal save failure. This mitigation still needs lifecycle execution tests.
- **P2 — blocked reader download:** explicit detail load no longer waits for the full sequential refresh loop. Cache commits re-read the current row/revision and current byte count.
- **P2 — wrong reminder target:** reminder dialogs capture their own article ID instead of reading whichever article navigation selected later.
- **P2 — repeated source switching:** sample/live switching takes `busy` before suspension and owns clear, repository replacement and refresh as one operation.
- **P2 — replayed notification intent:** activity recreation restores only pending navigation; consumed notification extras are removed instead of replayed on rotation.
- **Navigation ID collision:** internal article targets are prefixed before routing, keeping a valid article ID of `library` distinct from the Library destination. Intent article IDs themselves remain unchanged.
- **P2 — personal-data races:** bookmark/memo writes, memo deletion, reminder mutation and full personal-data deletion share `PersonalDataGate`.
- **P2 — missed-reminder recovery:** worker-first reboot reconciliation and multiple-overdue-worker coalescing no longer depend on the foreground Activity winning a race.
- **P2 — oversized notification payload:** summary extras are bounded to five displayed IDs/title snippets and a total count. Full titles for every overdue reminder are no longer sent through Binder. The OS summary count is a delivery snapshot; the Library remains the complete source.
- **P2 — discard prompt recreation:** `confirmDiscard` now uses `rememberSaveable` alongside `pendingTarget`, preserving the dirty-editor navigation prompt across recreation rather than leaving an invisible stale destination.

## Source review status

All concrete findings raised during this review have corresponding fixes in the final source reread. No additional critical source regression was identified in the final changes. Verification coverage and remaining limits are separated below; this is not approval for distribution.

## Final-change reread

- `ReaderWebView.kt`: a missing/broken provider's `RuntimeException` produces an inert, selectable cached-plaintext fallback. The fallback does not interpret article HTML or enable links/scripts. Restricted WebView settings, HTTPS-only navigation and SSL-error cancellation remain intact.
- `AndroidView.onRelease` detaches children before stopping/destroying the WebView and does not mutate Compose state during disposal. System font scale is applied through `textZoom` when the view is created.
- `AxApp.kt`: Back and incoming navigation call `hasUnsavedChanges()` at callback time, avoiding a stale recomposition-time dirty flag. Scaffold insets are consumed before the editor applies IME padding.
- Six small drawable vectors replace the extended icon dependency; the remaining Kotlin icons come from the core set. All replacement XML resources exist and parse. The current merged app manifest declares only `MainActivity`; preview/test-only activities are absent.
- No app code was changed by this reviewer during this final pass; only this report was updated.

## Focused checks actually executed

- Extracted both search queries from the Kotlin source, decoded their quote/backslash escapes and executed them in Python SQLite with bound parameters. The original SQL reproduced `ESCAPE expression must be a single character`. The corrected article and memo queries passed Korean partial text, literal `%_`, nonmatching wildcard-like text and literal backslash cases.
- Parsed every main-source XML file with Python ElementTree successfully. This checks XML syntax, not Android resource/manifest semantics or actual backup behavior.
- Checked delimiter balance in all current Kotlin sources after removing string/comment contents: no unmatched braces, parentheses or brackets. This is not a Kotlin/Compose compiler check.
- Final pass: parsed all 11 main-source XML files; inspected the packaged debug manifest and both generated Room schema-v1 exports. Backup exclusions and cleartext blocking remain configured; no storage/location/contacts permission is present.
- Compared bundled sample assets directly with `web-final/docs/app/v1`: all eight detail JSON files are byte-identical, and manifests match after excluding `generatedAt`.

## Security/data observations

Separate Room databases preserve personal snapshots across content clearing; no destructive personal migration fallback was found. Nullable article bodies and missing-content snapshots have UI fallbacks. Body HTML is sanitized, JavaScript/file/content access are disabled, links are HTTPS-only, SSL errors are cancelled, and notification intents are immutable and generation-specific. Automatic cloud backup and device transfer exclusions are explicitly configured. These are source observations, not device-level guarantees.

## Runtime evidence: reported execution, independently inspected artifacts

The implementing task reported successful compilation, Room/KSP generation, aggregate tests and lint for build8, three real-WebView API26 emulator flows on candidate7, and article-body rendering on candidate8. It also reported 47 passing web tests in each root/subpath configuration on upstream `6ffd29e21f292dda046b83664101f2e597f734ee`; this reviewer did not rerun those web suites.

During the final reread, build9 completed. The reviewer inspected, but did not execute:

- `android-build-9.log`: aggregate `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` finishes `BUILD SUCCESSFUL`.
- Current JUnit XML: **52 tests, 0 failures, 0 errors, 0 skipped**, with run timestamps 11:13 UTC; includes 3 Robolectric UI tests.
- Current lint XML: **0 errors, 12 warnings** (4 dependency-update suggestions, 6 KTX-style suggestions, 2 string/translation suggestions). This supersedes build8's 8-warning count.
- API26 Google APIs instrumentation transcript: **OK (3 tests)** for repeated navigation, dirty-editor Back/continue/discard and literal search states. The implementing task associates this transcript with candidate7; it is not proof that build9 has already passed on-device execution.

The inspected build9 app APK SHA-256 is `b67160f935d66c455350ad310efa355df865cc6b4c6dee95d3cbe680654b42b2`. Candidate8 body rendering remains parent-reported evidence. Later emulator results must identify the APK they actually exercised rather than silently inheriting earlier candidate coverage.

## Remaining verification boundary

The initial review preceded SDK approval/setup; that blocker is no longer current. Android compilation, tests and emulator interactions were **not run by this reviewer**; the reported executions and artifact inspection above are explicitly distinguished from the reviewer's focused checks. No physical-device validation is claimed. Isolated coordinator-only harness results are not additional Android test cases.

Build9's own emulator regression was still pending at this review handoff. Track uncovered lifecycle/save/rotation/deep-link flows, no-provider fallback regression, dialog target capture, rapid source switching, private-data deletion races, offline/cache removal, modern notification permission/channel denial, reboot/clock/force-stop recovery, notification taps, large text/TalkBack and signed-upgrade retention. The three API26 instrumentation flows are not coverage of that entire matrix. Physical-device behavior and representative performance remain unverified; record subsequent results in the test/emulator reports with their exact build identity.

## Subsequent clean-build result recorded by implementation owner

After the reviewed incremental build, `clean assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` executed all 86 tasks successfully. Regenerated test XML totals 52 passed, 0 failures/errors/skips. Final regenerated lint XML contains 8 warnings and 0 errors; the earlier four online GradleDependency suggestions did not appear in this clean report. The final APK is 10,474,638 bytes, SHA-256 `b67160f935d66c455350ad310efa355df865cc6b4c6dee95d3cbe680654b42b2`. This paragraph records the owner's later tool evidence, not an additional independent rerun. Final emulator scope is maintained in EMULATOR_REPORT.md.

## Approved editorial follow-up review — 2026-10-10

This section supersedes the older visual direction and candidate-specific automated counts above for the editorial follow-up to `f2221fa`. The independent reviewer inspected the approved concept's actual pixels and final production, test and documentation changes.

**No blocking source or security findings remain.** Earlier review feedback was resolved: compact sans article titles and spacing, a selectable Library tab group, a direct margin-note save/discard regression, and true Android font-scale masthead coverage. The review covered feed/collection navigation, bookmarks, memo protections, reminder preservation, tablet policy, native fallback selection/leading and OFL packaging.

The reader's font interception serves only the fixed packaged font for its exact GET subresource. Synthetic-host variants are blocked, CSP remains restrictive, title/taxonomy text is escaped, and JavaScript/file/content/mixed-content restrictions remain intact. No fabricated reading progress or remote font service was introduced. The narrowly scoped `ResourceType` suppression documents valid raw-byte access to the packaged font; it does not hide unrelated resource checks.

Checks executed by the independent reviewer:

- `git diff --check` for authored files; the verbatim upstream OFL license contains original trailing spaces and is excluded from that formatting check.
- Generated-token drift check
- All 11 Python token exporter/mutation tests

Implementation artifacts independently inspected, not rerun by the reviewer:

- Aggregate Gradle log: `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest compileReleaseKotlin` completed successfully.
- JUnit XML: 16 suites, **122 tests**, zero failures, errors or skips.
- Lint XML: zero errors and nine advisories (five KTX suggestions, four update notices for unchanged dependencies).
- Debug/instrumentation APK assembly and release Kotlin compilation.
- Independently calculated debug APK SHA-256: `edd2ba81caac1bb57bf3e60072fe5cd7c6487df8f6529be4dbbfa2d18227be01`.

The reviewer inspected the final light-phone, dark-reader, 200%-text and tablet split screenshots and the pulled-back installed-APK hash. These captures pass visual review: the serif/sans hierarchy, thin rules, toolbar/footer and compact navigation render without overlay, overlap or unintended clipping. Large-text metadata wraps; rows below the viewport remain scrollable. The dark article is rendered by the actual WebView, not a preview. Content was explicitly imported from the current public snapshot because emulator DNS failed; this is cached-content visual evidence only.

Visual evidence and remaining runtime limits are recorded in [RELEASE_0.3.0.md](RELEASE_0.3.0.md). Automated, source and software-emulator visual review are not physical-device or TalkBack certification.
