# AX website font and icon assets

Verified 2026-10-11 UTC against the live site and the companion repository. Native assets are original upstream files, without conversion, glyph removal or outline edits. The website supplies Noto Serif KR 600 for display, IBM Plex Sans KR for UI, and Pretendard Variable for article prose. The app preserves readable mobile sizes rather than copying desktop sizing/tracking.

## Bundled font provenance

### ibm_plex_sans_kr_medium.ttf

- [Official source](https://github.com/google/fonts/blob/main/ofl/ibmplexsanskr/IBMPlexSansKR-Medium.ttf)
- Family: IBM Plex Sans KR Medium; weight 500; Version 1.001
- Size: 2,866,736 bytes
- SHA-256: `4a7130f56ce50bf10f9d383f624e50bad2c80bebc63e51f4269ece2c7d919166`
- Git blob SHA-1: `4f646f9e23a82ec8e8fd2eae4c24ff9446eb2880`
- Modern Hangul: 11,172 / 11,172; printable ASCII: 95 / 95
- Jamo U+1100–11FF: 0 / 256; assigned compatibility Jamo: 94 / 94; basic Hanja: 0

### ibm_plex_sans_kr_regular.ttf

- [Official source](https://github.com/google/fonts/blob/main/ofl/ibmplexsanskr/IBMPlexSansKR-Regular.ttf)
- Family: IBM Plex Sans KR; weight 400; Version 1.001
- Size: 2,797,100 bytes
- SHA-256: `53750379270312368cf7641901f43a98dd892e3d9d5798cf25cdc245c85c71c0`
- Git blob SHA-1: `71f8a43b5763be2bc881c6d957c3111bd842d783`
- Modern Hangul: 11,172 / 11,172; printable ASCII: 95 / 95
- Jamo U+1100–11FF: 0 / 256; assigned compatibility Jamo: 94 / 94; basic Hanja: 0

### ibm_plex_sans_kr_semibold.ttf

- [Official source](https://github.com/google/fonts/blob/main/ofl/ibmplexsanskr/IBMPlexSansKR-SemiBold.ttf)
- Family: IBM Plex Sans KR SemiBold; weight 600; Version 1.001
- Size: 2,854,744 bytes
- SHA-256: `823c1956adf56c27c06b062725dfce23f266e4fd1366290f2957c8407482a817`
- Git blob SHA-1: `860bf04b2e79a1714f3dbeb725bc077620998fc5`
- Modern Hangul: 11,172 / 11,172; printable ASCII: 95 / 95
- Jamo U+1100–11FF: 0 / 256; assigned compatibility Jamo: 94 / 94; basic Hanja: 0

### noto_serif_kr_semibold.otf

- [Official source](https://github.com/notofonts/noto-cjk/blob/main/Serif/SubsetOTF/KR/NotoSerifKR-SemiBold.otf)
- Family: Noto Serif KR SemiBold; weight 600; Version 2.003;hotconv 1.1.1;makeotfexe 2.6.0
- Size: 7,610,748 bytes
- SHA-256: `a63051d299d7219132a98ff3c2b76a62022344e50e9f3b50fdb9b0ee4560f583`
- Git blob SHA-1: `91680563fa379075747e83a8ee940b02b996333d`
- Modern Hangul: 11,172 / 11,172; printable ASCII: 95 / 95
- Jamo U+1100–11FF: 256 / 256; assigned compatibility Jamo: 94 / 94; basic Hanja: 8138

### pretendard_variable.ttf

- [Official source](https://github.com/orioncactus/pretendard/blob/main/packages/pretendard/dist/public/variable/PretendardVariable.ttf)
- Family: Pretendard Variable; weight 400; Version 1.309;Glyphs 3.2 (3225)
- Size: 6,739,336 bytes
- SHA-256: `3090ccde0442bb347aa7685d9ba8b17436a60682df6e8f92a9a670de14056e22`
- Git blob SHA-1: `32b0811eae2cbfeb5f6ddaf34015ecaa726b006a`
- Modern Hangul: 11,172 / 11,172; printable ASCII: 95 / 95
- Jamo U+1100–11FF: 0 / 256; assigned compatibility Jamo: 53 / 94; basic Hanja: 0

The Noto resource is the publisher's official Korean regional subset, not an app-generated subset. It is registered at weight 600 even though its internal subfamily says Regular. Pretendard's original variable weight range is retained. Android and CSS system fallbacks remain enabled for characters absent from these sans-serif resources. This is modern-Hangul coverage, not a claim of universal Korean-script or CJK coverage.

## Copyright and licensing

All three families use SIL OFL 1.1. Original license files are bundled under `app/src/main/assets/licenses`, available in Settings, and preserved unchanged. Copyright embedded in original files includes:

- Noto Serif KR: © 2017–2024 Adobe (http://www.adobe.com/). [Upstream license](https://github.com/notofonts/noto-cjk/blob/main/Serif/LICENSE)
- IBM Plex Sans KR: Copyright 2018 IBM Corp. All rights reserved. The accompanying [OFL notice](https://github.com/google/fonts/blob/main/ofl/ibmplexsanskr/OFL.txt) lists Copyright © 2017 IBM Corp. and reserved name Plex.
- Pretendard Variable: Copyright © 2023 Kil Hyung-jin. Its [distribution notice](https://github.com/orioncactus/pretendard/blob/main/packages/pretendard/dist/LICENSE.txt) also preserves the upstream Pretendard, Adobe Source and Inter notices.

## Existing AX icon geometry

Source: [the owner's favicon SVG](https://github.com/msaltnet/ax-notes-public/blob/main/public/favicon.svg). The live site links matching 16px/32px PNGs and an 180px touch icon; the SVG is the reusable path source. User-requested reuse of their brand asset does not imply a third-party public-domain license.

`ax_brand_mark.xml`, `ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml` reuse its two original path strings and overlap. The original cream #f3f0e7 and near-black #191915 remain in the launcher. Adaptive foreground raw bounds fit in x=27.343–80.657, y=36.865–71.135 of the 108×108 canvas, inside the central 66dp circle even at bounding-box corners. Background is opaque/full-bleed, foreground is transparent apart from glyphs, and Android applies its own masks. Minimum supported Android API 26 resolves the adaptive resource; API 33 adds the same geometry as a monochrome layer.

The in-app mark uses semantic foreground colors for light/dark contrast. It is decorative within one accessible “AX Notes” masthead rather than exposing each path or letter separately.
