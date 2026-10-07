# AX Notes MVP verification report

Date: 2026-10-07. This is an internal-test candidate, not a production release or a real-device certification.

## Pinned source baselines

- Android: `msaltnet/ax-notes` main `872827c4129ccfcc6da46fa05d546f685166a69e`
- Final web: `msaltnet/ax-notes-public` `6ffd29e21f292dda046b83664101f2e597f734ee`
- Initial web work used `a9c27bbc3985cc3dd4ad5d3aff970768ec79c956`. The patch was reapplied in a new directory to 6ffd29e, preserving the upstream AstroContainer/OG-image changes; the original checkout was not reset
- No commit, push, PR, store upload or deployment was performed

## Android build and automated checks: passed

Toolchain: Eclipse Temurin JDK 17.0.20.1+1, Gradle 8.11.1, AGP 8.9.2, Kotlin/Compose compiler 2.1.20, compile/target SDK 35, minimum SDK 26, build-tools 35.0.0. SDK terms were accepted at 10:40 UTC after the user's approval. Gradle and JDK downloads were checked against official SHA-256 values.

Executed aggregate: `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` with Gradle 8.11.1. The cloud used a writable test/cache home and its existing system Java trust store/proxy. No TLS verification was disabled.

- Kotlin/Java compilation and Room/KSP generation: passed
- Room content and personal schema v1 JSON exports: generated
- App APK assembly and instrumentation APK assembly: passed
- 52 JVM/Robolectric tests: 52 passed, 0 failed, 0 errors
  - Feed/schema/security/sample contract: 7
  - Repository/cache/search/personal invariants: 7
  - Database reopen and idempotent memo retry: 2
  - Compose navigation/dirty Back/search: 3
  - Reminder state, races and recovery: 25
  - Android notification channel/intent/payload behavior: 8
- Lint: 0 errors, 8 nonblocking warnings (6 KTX-style suggestions, 2 translation/string-composition suggestions for the Korean WebView fallback)
- APK signature verification: passed, APK Signature Scheme v2, one RSA 2048-bit Android Debug signer
- Final merged manifest: backups disabled, cloud/device-transfer exclusion rules present, cleartext traffic disabled. No storage/location/contacts permission. WorkManager merges wake-lock, network-state, boot, and foreground-service infrastructure permissions; this app schedules no foreground work or exact alarms
- Debug preview/test-only activities were removed from the delivered app dependency graph

Earlier isolated JVM checks (25 reminder + 7 feed/export) and extracted SQLite/XML checks passed during SDK setup, but are not counted as extra Android test cases.

## Web verification: passed on final baseline

- Default GitHub subpath: `npm run verify`, 47 tests passed
- Production root (`SITE_URL=https://ax.msalt.net BASE_PATH=/`): `npm run verify`, 47 tests passed
- Includes the new upstream OG-image regression, feed schema/revision/sanitization/link/image checks and existing site tests
- Synthetic draft through the real build is excluded
- Eight article summaries, bodies and revisions are byte/semantically unchanged from the APK's original verified snapshot; only final manifest.generatedAt changed, so the original bundled assets remain valid
- See the separate web repository's `docs-dev/app-feed-validation.md` for HTTP/image checks and exact scope

## Emulator execution

The exact final APK was installed on an API26 Google APIs emulator; the installed base.apk was pulled back and its SHA-256 matched. All 3 instrumentation flows passed. Real WebView local/cached HTML rendering, linked memo save/search, force-stop persistence, unbookmark independence and cache-only deletion preserving personal snapshots were verified. Full evidence is in `EMULATOR_REPORT.md`. Strict network-disabled offline operation was not established because Wi-Fi remained connected. API35 and physical-device execution were not run. Software emulation is not a representative performance benchmark.

An early API26 AOSP image had no WebView provider. The initial reader launch failed; the underlying exception was found before a secondary Compose disposal exception. This was fixed: missing/broken WebView now shows selectable cached plaintext with an explicit image/formatting limitation. The final candidate also reads current editor state at Back time, handles IME insets, scales WebView text with system font size and releases the WebView without changing Compose state during disposal.

## APK identity and signing

- Version: `0.1.0-internal`, versionCode 1
- Package: `net.msalt.axnotes.internal`
- Android 8.0/API26 minimum; universal APK
- Debug signer certificate SHA-256: `f4d01f3c264affe0b7747db7cff032d9c17052e2ec07bc6414d9a54ad9700920`
- APK SHA-256 and exact byte size: see the delivered `SHA256SUMS.txt` / release notes
- The private signing key is not in the source archive. This is not a production signing setup. An update to the same package requires the same signing key; a new key may require uninstalling, which loses private data. A production package will not automatically inherit this internal package's data

## Explicit remaining limits

- No physical Android device was connected or tested
- No claim of exact alarm timing, guaranteed OS notification delivery or physical exactly-once display
- No physical-device search p95/first-list benchmark for 1,000 articles + 1,000 memos
- No older released database schema exists; schema-v1 reopen persistence is tested, not a fictional v0-to-v1 migration
- Device-specific OEM backup/transfer behavior and a real signed-version upgrade remain physical-device checklist items unless explicitly covered in the emulator appendix
- Web JSON remains local/unpublished. Live mode can fail until the owner separately publishes the web changes; default internal mode is clearly labelled sample content
- Images are fetched over HTTPS and complete offline image availability is not guaranteed
- Account, backup, sync, export, Labs and advertising are outside this MVP
