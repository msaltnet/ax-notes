#!/usr/bin/env python3
"""Export the actual Kotlin token definitions; --check rejects stale design handoffs.

This JSON is a documented handoff, not a claim of a Figma import or plugin format.
Only literal token declarations and named references are accepted. No Kotlin is run.
"""
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/net/msalt/axnotes/ui"
OUT = ROOT / "design/ax-tokens.json"


def export():
    source = (UI / "AxDesignSystem.kt").read_text()
    tokens = {
        "$description": "AX editorial paper/ink design. Generated from Kotlin; see docs/DESIGN_SYSTEM.md for reference and licensing.",
        "color": {}, "dimension": {}, "typography": {}, "reader": {}, "semantic": {},
    }
    for mode, name in (("light", "AxLightColors"), ("dark", "AxDarkColors")):
        block = re.search(rf"internal val {name} = \w+ColorScheme\((.*?)\n\)", source, re.S)
        assert block, name
        colors = {}
        for line in block[1].splitlines():
            line = line.split("//", 1)[0].strip()
            if not line:
                continue
            match = re.fullmatch(r"(\w+) = Color\(0xFF([0-9A-Fa-f]{6})\),?", line)
            if not match:
                raise ValueError(f"Unsupported color declaration: {line}")
            key, color = match.groups()
            colors[key] = {"$type": "color", "$value": "#" + color.lower()}
        tokens["color"][mode] = colors
        assert len(tokens["color"][mode]) >= 30, f"incomplete {mode} palette"

    dimensions = {}
    for name in ("AxSpacing", "AxSize", "AxComponentTokens"):
        block = re.search(rf"object {name} \{{(.*?)\n\}}", source, re.S)
        assert block, name
        values = {}
        for line in block[1].splitlines():
            line = line.split("//", 1)[0].strip()
            if not line:
                continue
            declaration = re.fullmatch(r"val (\w+) = (.+)", line)
            if not declaration:
                raise ValueError(f"Unsupported dimension declaration: {line}")
            key, raw = declaration.groups()
            raw = raw.strip()
            if re.fullmatch(r"\d+(?:\.\d+)?\.dp", raw):
                value = float(raw[:-3])
            elif raw in dimensions:
                value = dimensions[raw]
            else:
                raise ValueError(f"Unsupported token expression: {name}.{key} = {raw}")
            dimensions[f"{name}.{key}"] = value
            values[key] = {"$type": "dimension", "$value": {"value": value, "unit": "dp"}}
        tokens["dimension"][name] = values

    for role, style in re.findall(r"(\w+) = TextStyle\((.*?)\)", source):
        attributes = {}
        for item in style.split(","):
            attribute = re.fullmatch(r"\s*(\w+)\s*=\s*(.*?)\s*", item)
            if not attribute or attribute[1] in attributes:
                raise ValueError(f"Unsupported or duplicate typography attribute in {role}: {item}")
            attributes[attribute[1]] = attribute[2]
        allowed = {"fontFamily", "fontWeight", "fontSize", "lineHeight", "letterSpacing"}
        if set(attributes) - allowed:
            raise ValueError(f"Unsupported typography attributes in {role}: {set(attributes) - allowed}")

        def sp_value(name):
            match = re.fullmatch(r"(-?\d+(?:\.\d+)?)\.sp", attributes.get(name, ""))
            if not match:
                raise ValueError(f"Explicit literal sp {name} required in {role}")
            return float(match[1])

        families = {"SansSerif": "sans-serif", "Serif": "serif", "Monospace": "monospace", "Cursive": "cursive", "Default": "system-ui"}
        family_match = re.fullmatch(r"FontFamily\.(\w+)", attributes.get("fontFamily", ""))
        if attributes.get("fontFamily") == "AxEditorialFont":
            if "internal val AxEditorialFont = FontFamily(Font(R.font.nanum_myeongjo_regular, FontWeight.Normal))" not in source:
                raise ValueError("Unsupported editorial font family resource")
            family = "Nanum Myeongjo"
        elif family_match and family_match[1] in families:
            family = families[family_match[1]]
        else:
            raise ValueError(f"Unsupported font family in {role}")
        weights = {"Normal": 400, "Medium": 500, "SemiBold": 600, "Bold": 700}
        weight_match = re.fullmatch(r"FontWeight\.(\w+)", attributes.get("fontWeight", "FontWeight.Normal"))
        if not weight_match or weight_match[1] not in weights:
            raise ValueError(f"Unsupported font weight in {role}")
        tokens["typography"][role] = {"$type": "typography", "$value": {
            "fontFamily": family, "fontWeight": weights[weight_match[1]],
            "fontSize": {"value": sp_value("fontSize"), "unit": "sp"},
            "lineHeight": {"value": sp_value("lineHeight"), "unit": "sp"},
            "letterSpacing": {"value": sp_value("letterSpacing"), "unit": "sp"},
        }}
    assert len(tokens["typography"]) == 15
    reader_source = (UI / "AxReaderStyle.kt").read_text()
    for line in reader_source.splitlines():
        line = line.split("//", 1)[0].strip()
        if not line.startswith("const val "):
            continue
        match = re.fullmatch(r"const val (\w+) = (\d+)", line)
        if not match:
            raise ValueError(f"Unsupported reader token declaration: {line}")
        key, value = match.groups()
        tokens["reader"][key] = {"$type": "number", "$value": int(value)}
    tokens["reader"]["$description"] = "Font sizes/line heights: CSS px or native sp. Layout: CSS px or native dp. horizontalPaddingViewportPercent is a percentage. See AxReaderStyle.kt."
    role_map = {"canvas": "background", "text": "onSurface", "supportingText": "onSurfaceVariant",
                "action": "primary", "onAction": "onPrimary", "actionContainer": "primaryContainer",
                "onActionContainer": "onPrimaryContainer", "card": "surface",
                "selected": "secondaryContainer", "onSelected": "onSecondaryContainer",
                "border": "outlineVariant", "error": "error", "onError": "onError"}
    for mode in ("light", "dark"):
        tokens["semantic"][mode] = {key: {"$type": "color", "$value": "{color." + mode + "." + value + "}"}
                                    for key, value in role_map.items()}
    return json.dumps(tokens, ensure_ascii=False, indent=2) + "\n"


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    result = export()
    if args.check:
        if not OUT.exists() or OUT.read_text() != result:
            raise SystemExit("Design token export is stale. Run scripts/export-design-tokens.py")
        print("Design token export matches Kotlin definitions")
    else:
        OUT.parent.mkdir(parents=True, exist_ok=True)
        OUT.write_text(result)
        print(f"Wrote {OUT.relative_to(ROOT)}")
