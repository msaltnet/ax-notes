# Editorial app captures

These are unedited screenshots of the actual Android app on a disposable cloud emulator, captured on 2026-10-10 (UTC). They are not generated mockups or Compose preview renders.

## Build and data provenance

- Package: `net.msalt.axnotes.internal`, version `0.3.0`, code `3`.
- APK SHA-256: `edd2ba81caac1bb57bf3e60072fe5cd7c6487df8f6529be4dbbfa2d18227be01`.
- The APK was installed with replacement/update semantics. Its installed `base.apk` was pulled back and independently hashed to the same value.
- Android API 26, x86, software emulation; WebView provider Chrome `69.0.3497.100`.
- Phone display: 720 × 1560 physical pixels, 280 dpi (approximately 411 × 891 dp).
- Tablet display: 1280 × 800 physical pixels, 160 dpi (1280 × 800 dp), system text 100%.
- Source content: nine currently public articles from [the AX Notes manifest](https://ax.msalt.net/app/v1/manifest.json) and its linked detail JSON, fetched over HTTPS by the cloud host on 2026-10-10.
- Manifest snapshot SHA-256: `021de9addff63f846c357ab54cc59f3e603fc6d49d96b141a9581090337a2b6e`.
- The emulator could not resolve `ax.msalt.net` (`EAI_NODATA`). Its live-feed instrumentation attempt failed before import or creation of personal test records. For visual QA, the exact public snapshot was imported into the emulator's content cache. No bookmarks, memos or reminders were imported; the reader correctly shows zero linked memos.
- No production source, trust store, network-security policy or feed setting was weakened for these captures. Host HTTPS availability is not an emulator-network pass.

## Screens

- [Phone, light Notes](phone-light-notes.png): serif journal heading, underlined sections, ruled article rows, current taxonomy and the compact four-destination navigation.
- [Phone, dark reader](phone-dark-reader.png): actual WebView article text, bundled serif headline/subheading, dark reading palette and cache/memo margin actions.

- [Phone, 200% system text](phone-light-200pct.png): enlarged journal/article copy and wrapping metadata, with the compact navigation still visible. The scrollable article list extends below the captured viewport.

- [Tablet, light split view](tablet-light-split.png): navigation rail, 400dp list pane with a tinted/ruled selected article, and actual WebView title/body with separate actions and margin notes.

## Verification limits

Software-emulator startup produced a System UI ANR overlay during optimization and an initially blank WebView frame. Captures were taken only after those startup frames settled; the final images contain no overlay. The installed app's cached article renders in a real WebView. This is neither representative performance evidence nor certification of current WebView versions.

Live emulator networking, remote images, physical-device behavior, TalkBack and a production-signed upgrade remain unverified. See [release verification](../RELEASE_0.3.0.md) and [independent review](../INDEPENDENT_REVIEW.md) for the executed automated checks and review scope.
