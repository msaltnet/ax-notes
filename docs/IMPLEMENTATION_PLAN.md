# MVP implementation plan · 2026-10-07

## Baseline and scope
- Android repository: `872827c4129ccfcc6da46fa05d546f685166a69e`, initially README/PRD/TRD only.
- Initial separate web checkout: `a9c27bbc3985cc3dd4ad5d3aff970768ec79c956`. Final integration was safely reapplied in another directory to `6ffd29e21f292dda046b83664101f2e597f734ee`, preserving upstream OG-image/AstroContainer changes. Both root and subpath verification were rerun.
- User confirmed there are no uncommitted changes requiring recovery. Work happens in fresh, separate cloud checkouts; no user computer is modified.
- No remote push, PR, deployment, store upload, account, analytics, advertisement, Labs screen, backup, device transfer, or independent memo.

## Decisions before implementation
1. Kotlin + Compose, a single Android module. Android-only native storage, permission, lifecycle and scheduler APIs avoid cross-platform overhead. A restricted WebView is only the article renderer.
2. Separate Room content and personal databases. Bookmark and Memo are different tables so one bookmark and multiple memos per article coexist. No destructive personal migration fallback.
3. WorkManager one-time work and persisted reminder generations. The time is a earliest execution instant, never an exact alarm promise. Stable notification identities make repeated execution idempotent; DB and OS posting are not one transaction, so claims are recoverable.
4. SQLite bound substring queries with literal escaping, Unicode normalization, local debounce, and a 200-character input limit. Korean partial matching is more important than token-based FTS for this MVP.
5. JSON v1: complete manifest + revision-addressed detail files. HTTPS origin/path validation, byte limits, schema/id/revision checks. Desired manifest revision remains separate from cached body revision; failed downloads never relabel old HTML.
6. Package `net.msalt.axnotes`; internal variant adds `.internal`. API 26 minimum, compile/target 35 for this internal baseline. Pinned AGP 8.9.2 / Gradle 8.11.1 / Kotlin 2.1.20 are an intentionally conservative mutually compatible toolchain, not a claim to latest versions or Play readiness. AGP compatibility: https://developer.android.com/build/releases/agp-8-9-0-release-notes . Compose compiler uses the matching Kotlin plugin: https://developer.android.com/jetpack/androidx/releases/compose-kotlin .
7. Internal APK is debug-signed locally; it is not a production release. Never distribute a private signing key in the source archive. Record certificate fingerprint and APK SHA-256, disclose future upgrade-key continuity.
8. Production feed defaults to `https://ax.msalt.net/app/v1/manifest.json`. Because publication is prohibited, the internal APK includes a clearly labelled sample-content mode sourced from the separately verified local web export. Live mode is explicit; no fake live-success claim.

## Sequence and ownership
1. Write this plan, freeze API contract, install authorized official tools (SDK license approved and accepted later at 10:40 UTC; see decision log).
2. Web export and contract verification independently in the separate web checkout. Preserve the current publication filter and image pipeline; validate root and subpath URLs; export fixture bytes.
3. Android data layer, private-data invariants, cache/sync/limits; memo/search; UI reader/library/settings; reminder state machine.
4. Run unit/integration tests, lint, APK assembly and independent review; fix failures and rerun relevant aggregate checks.
5. Package APK, source patches/archive, decision log and verification report. List real-device checks separately and honestly.

## Verification gates
- Web: existing verify + schema/revision/draft/security/asset/subpath contract tests.
- Android: sync atomicity, stale revision behavior, unsupported versions, overlimit input, Korean/wildcard search, personal data after cache reset and unavailable content, multiple memos, reminder generation/cancellation/recovery, backup exclusions, restricted WebView and navigation safety.
- Device/emulator: repeat navigation, Back and unsaved editor, permission denied/channel disabled, reboots/clock changes/force-stop, notification tap, offline body, large text/TalkBack, upgrade data retention. Not considered passed unless actually executed.

## Known publication limit
New endpoints can be verified locally but cannot exist on production until the owner later authorizes and performs deployment. Immutable detail paths reduce CDN skew; deployment must still retain prior revisions long enough for older manifests.
