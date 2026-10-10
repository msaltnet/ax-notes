"""Mutation checks prove that the design handoff cannot silently drift from Kotlin."""
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("ax_token_export", Path(__file__).with_name("export-design-tokens.py"))
EXPORTER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(EXPORTER)
READ_TEXT = Path.read_text


class DesignTokenExportTest(unittest.TestCase):
    def mutated_export(self, filename, before, after):
        def read(path, *args, **kwargs):
            text = READ_TEXT(path, *args, **kwargs)
            if path.name == filename:
                self.assertIn(before, text)
                return text.replace(before, after)
            return text
        with patch.object(Path, "read_text", read):
            return json.loads(EXPORTER.export())

    def test_checked_in_export_matches_kotlin(self):
        self.assertEqual(EXPORTER.OUT.read_text(), EXPORTER.export())

    def test_letter_spacing_changes_are_exported(self):
        result = self.mutated_export("AxDesignSystem.kt", "letterSpacing = 0.sp", "letterSpacing = 1.5.sp")
        self.assertEqual({"value": 1.5, "unit": "sp"}, result["typography"]["bodyLarge"]["$value"]["letterSpacing"])
        self.assertNotEqual(json.loads(EXPORTER.export()), result)

    def test_font_family_changes_are_exported(self):
        result = self.mutated_export("AxDesignSystem.kt", "FontFamily.SansSerif", "FontFamily.Serif")
        self.assertEqual("serif", result["typography"]["bodyLarge"]["$value"]["fontFamily"])
        self.assertNotEqual(json.loads(EXPORTER.export()), result)

    def test_unsupported_letter_spacing_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "letterSpacing"):
            self.mutated_export("AxDesignSystem.kt", "letterSpacing = 0.sp", "letterSpacing = 0.1.em")

    def test_unsupported_font_family_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "font family"):
            self.mutated_export("AxDesignSystem.kt", "FontFamily.SansSerif", "FontFamily.Custom")

    def test_color_expression_cannot_be_silently_omitted(self):
        with self.assertRaisesRegex(ValueError, "color declaration"):
            self.mutated_export("AxDesignSystem.kt", "primary = Color(0xFF315E50)", "primary = Color.Green")

    def test_dimension_division_cannot_be_mistaken_for_a_comment(self):
        with self.assertRaisesRegex(ValueError, "token expression"):
            self.mutated_export("AxDesignSystem.kt", "val icon = 24.dp", "val icon = 24.dp / 2")

    def test_typography_expression_cannot_export_only_its_first_number(self):
        with self.assertRaisesRegex(ValueError, "fontSize"):
            self.mutated_export("AxDesignSystem.kt", "fontSize = 16.sp", "fontSize = 16.sp * 2")

    def test_new_typography_attribute_cannot_be_silently_omitted(self):
        with self.assertRaisesRegex(ValueError, "typography attributes"):
            self.mutated_export("AxDesignSystem.kt", "letterSpacing = 0.sp", "letterSpacing = 0.sp, fontStyle = FontStyle.Italic")

    def test_reader_expression_cannot_export_only_its_first_number(self):
        with self.assertRaisesRegex(ValueError, "reader token"):
            self.mutated_export("AxReaderStyle.kt", "const val bodyFontSize = 18", "const val bodyFontSize = 18 + 1")


if __name__ == "__main__":
    unittest.main()
