from __future__ import annotations

import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

from json_bench_python.cli import main


class CliTest(unittest.TestCase):
    def run_cli_and_capture(self, *args: str) -> str:
        buffer = io.StringIO()
        with redirect_stdout(buffer):
            main(list(args))
        return buffer.getvalue()

    def test_prints_help_for_help_command(self) -> None:
        output = self.run_cli_and_capture("help")
        self.assertIn("Usage: <command> <file> [command-args...]", output)
        self.assertIn("length <file> [expr]", output)
        self.assertIn("filter <file> [expr]", output)
        self.assertIn("map <file> [expr]", output)

    def test_prints_help_for_h_command(self) -> None:
        output = self.run_cli_and_capture("h")
        self.assertIn("Commands:", output)
        self.assertIn("help | h", output)

    def test_suggests_help_when_no_command_is_provided(self) -> None:
        output = self.run_cli_and_capture()
        self.assertIn("No command provided.", output)
        self.assertIn("Use 'help' or 'h'", output)

    def test_suggests_help_for_unknown_command(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            input_path = Path(tmp_dir) / "sample.json"
            input_path.write_text("{}", encoding="utf-8")

            output = self.run_cli_and_capture("unknown", str(input_path))
            self.assertIn("Unknown command: unknown", output)
            self.assertIn("Use 'help' or 'h'", output)

    def test_prints_input_file_not_found(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            missing_path = Path(tmp_dir) / "missing.json"
            output = self.run_cli_and_capture("length", str(missing_path))
            self.assertIn("Input file not found:", output)
            self.assertIn(str(missing_path), output)

    def test_runs_length_command_end_to_end(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            input_path = Path(tmp_dir) / "input.json"
            input_path.write_text("[1,2,3]", encoding="utf-8")

            output = self.run_cli_and_capture("length", str(input_path))
            self.assertEqual("3", output.strip())

    def test_runs_length_command_with_filter_expression_end_to_end(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            input_path = Path(tmp_dir) / "input.json"
            input_path.write_text(json.dumps({"users": [1, 2, 3], "meta": True}), encoding="utf-8")

            output = self.run_cli_and_capture("length", str(input_path), ".users")
            self.assertEqual("3", output.strip())


if __name__ == "__main__":
    unittest.main()
