# PR 8 owner-feedback and website-brand evidence

Captured 2026-10-11 UTC. A selected subset was delivered separately to the owner as private attachments; the complete list below records reviewed captures retained outside the repository. No new PNG is published here. All are original lossless renders with no retouching or invented UI. This text records the evidence for the current font/icon and feedback changes.

## Supplementary native Android/Compose renders

These are **Robolectric 4.14.1 native-Skia renders, not emulator or physical-device screenshots**. MainActivity or the exact production reminder dialog was drawn through its actual Android decor view and Canvas. They do not prove real keyboard, WebView or window-manager behavior.

- Settings, light, 411dp/100% — `settings-light-native.png` (private verification image)
- Settings, dark, 360dp/200% — `settings-dark-200-native.png` (private verification image)
- Settings, light, 411dp/200% — `settings-light-200-native.png` (private verification image)
- Numeric reminder, 360dp/200% — `reminder-time-200-native.png` (private verification image)
- Date input, 360dp/200% — `reminder-date-200-native.png` (private verification image)
- Adaptive AX icon, API 28 circle — `launcher-circle-native.png` (private verification image)
- Adaptive AX icon, API 28 squircle — `launcher-squircle-native.png` (private verification image)
- API 33 monochrome vector — `launcher-monochrome-native.png` (private verification image)

Korean/Republic of Korea locale, Asia/Seoul timezone and mdpi. Reminder controls use the explicit example 2026-10-13 20:30 Asia/Seoul; no reminder was submitted or scheduled. Settings uses the current public nine-note metadata cache, with no personal records. Its refresh time is the test cache's staging time, not a user activity record.

The icon mask images use the actual packaged AdaptiveIconDrawable under test-configured Android circle/squircle masks; the framework applies the mask and scale. The monochrome image is the original white vector on transparent pixels, not a claim of launcher-selected theme colors. API-33 adaptive raster output was blank in this native test runtime and is excluded; API-33 resource selection and geometry tests passed. Notes captures with a visible in-flight refresh are also excluded from this deliverable.

The separate capture harness passed 11/11 cases at 06:17 UTC and was removed before the final 143-test aggregate. Independent pixel review opened all eight selected PNGs and found no blocking clipping, overlap or glyph loss. At 200%, additional Settings sections remain available by scrolling. The 57 hashes record the captured production source/resources. After these captures, only AxReminderDialog.kt received the final date-window-height fix; the other 56 source/resource hashes, including every font, icon, masthead and Settings implementation, remain identical. The final date host is verified with the real keyboard below. [Source hashes](native-source-sha256.json), [PNG hashes](native-png-sha256.json).

## Runtime scope

The internal final debug APK SHA-256 is `df867b96d360ed4fe58f9b7e7a1608d8b0148d65501a5c4dc6b03be90bcb54c5`. The final APK was installed in-place on the disposable API-26 emulator with app data retained; the pulled installed base.apk independently hashes to the same value. The older intermediate build's keyboard-clipping screenshot is diagnostic evidence, not a final passing screenshot.

The preceding branding candidate `0c2eb23007a5265f8456b433d5bee1466aace11de9d4c46e75a4df11e6259efb` passed actual numeric-keyboard, minute-scroll and launcher-icon review, but its date picker remained partially covered by the IME. Those results motivated the final outer-window-height fix; they are not represented as a date pass or as captures of the newer APK. The missing-article reader frame only proves masthead/control rendering, not WebView article typography. Final WebView prose, current Android themed-launcher interaction, TalkBack and physical-device upgrade retention remain outside this verification.


### Final date keyboard verification, 07:21–07:22 UTC

At 200% system text, the final APK passes the date-input check with the real numeric keyboard shown and hidden. The entire selected date and both Cancel/Apply controls remain visible; the visible-keyboard action row ends near y530, above the IME starting at y585. With the keyboard hidden, the dialog is centered and all controls remain visible. Both the implementation owner and an independent reviewer opened the unedited PNGs. The actual input hint and date format follow the emulator's English locale; the application's Korean controls render without loss.

- `03-date-200-keyboard-hidden.png` — actual final-APK date dialog, keyboard hidden (private attachment).
- `04-date-200-keyboard-visible.png` — actual final-APK date dialog, keyboard visible (private attachment).

The emulator uses Android API 26 Google APIs x86 software rendering, a 540×960px display with a 210dpi override (approximately 411dp width), and font scale 2.0. Slow package optimization and initial stock System UI startup warnings are environment limitations, not performance evidence. No reminder was submitted and no personal record was created for this check.


### Final numeric keyboard verification, 07:23 UTC

The same hash-verified final APK also passes `07-time-200-keyboard-visible.png`: the complete hour/minute labels and entered values, Cancel and Apply all remain above the real keyboard at 200% text. Independent pixel review confirmed this final numeric frame. Therefore both date and numeric picker IME checks pass on the final APK; the older numeric/minute-scroll frames are retained only as historical diagnostics. The actual launcher render remains the preceding candidate's check, with unchanged icon resource bytes.

The three final raw PNG SHA-256 values are recorded in [runtime-png-sha256.json](runtime-png-sha256.json); the images themselves are private attachments, not repository assets.
