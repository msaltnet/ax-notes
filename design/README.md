# AX design handoff

The app's design-system source is Kotlin, not a disconnected mockup:

- `AxDesignSystem.kt`: paired light/dark colors, complete type scale, spacing, shapes and component dimensions.
- `AxComponents.kt`: actual reusable cards, action hierarchy, filters, input and status/empty components used by the app.
- `AxReaderStyle.kt`: long-form type/layout and HTML color mapping, shared with the native-text fallback.
- `AxDesignSystemCatalog.kt`: an executable catalogue of those same components and states. It is a development/test surface, not a new app destination.

## Open the actual catalogue

In Android Studio, choose the debug build variant and open `app/src/debug/java/net/msalt/axnotes/ui/AxDesignSystemPreviews.kt`. Its Compose previews render compact light/dark, 200% text and an expanded window. [Interactive Preview](https://developer.android.com/develop/ui/compose/tooling/previews#interactive-mode) lets you scroll through the catalogue and toggle its sample filter. The two tooling dependencies are debug-only, use the existing Compose BOM and are absent from the release runtime. Preview rendering in Android Studio itself is a developer workflow, not a claimed completed device test.

## Token export

Run `python scripts/export-design-tokens.py` after changing tokens, then `python scripts/export-design-tokens.py --check` before review. The generated [ax-tokens.json](ax-tokens.json) records values, units and semantic aliases without manually duplicating them. The script fails if a token declaration cannot be understood; it never executes Kotlin. Run `PYTHONDONTWRITEBYTECODE=1 python -m unittest discover -s scripts -p 'test_*.py'` to verify the export and mutation guards for font family, letter spacing, colors and reader token expressions.

The JSON is a handoff reference, **not a verified Figma plugin/import format**. No Figma file, variables collection or library has been created. A designer can map its semantic names to a light/dark Figma variables collection after the intended account/file is available. Typeface metrics use Android `sp`; layouts use `dp`. Reader tokens explain their CSS/native unit mapping separately.

## Catalogue state coverage

| Family | Variants / states |
| --- | --- |
| Action | primary, outlined, text, destructive; enabled, disabled, loading |
| Content card | default, selected; independent body and secondary actions |
| Filter | selected, unselected, disabled; long Korean label |
| Input | empty, filled, error with supporting text/icon, read-only, disabled |
| Status | information, warning, error, empty state with next-action guidance |

Focus, press/ripple and native disabled semantics come from Material 3 controls, rather than an independent interaction engine. Compact navigation and reading typography are deliberate AX adaptations. Existing tablet navigation, reader position and draft protections remain in the app's flow tests.

See [the design-system specification](../docs/DESIGN_SYSTEM.md) for the selected Figma Community kit, source attribution, AX deviations and verification limits.
