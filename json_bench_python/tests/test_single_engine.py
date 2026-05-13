from __future__ import annotations

import unittest

from json_bench_python.engine import SingleEngine


class SingleEngineTest(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = SingleEngine()

    def test_name_is_single(self) -> None:
        self.assertEqual("single", self.engine.name())

    def test_exposes_expected_commands(self) -> None:
        self.assertEqual({"length", "filter", "map"}, self.engine.command_names())

    def test_executes_length_command(self) -> None:
        input_data = {"users": [1, 2, 3]}
        output = self.engine.execute("length", input_data, [])
        self.assertEqual(1, output)

    def test_executes_filter_command(self) -> None:
        input_data = {"users": [{"id": 0}, {"id": 1}]}
        output = self.engine.execute("filter", input_data, [".users"])
        self.assertEqual([{"id": 0}, {"id": 1}], output)

    def test_executes_map_command(self) -> None:
        input_data = [1, 2, 3]
        output = self.engine.execute("map", input_data, ["+1"])
        self.assertEqual([2, 3, 4], output)

    def test_fails_for_unknown_command(self) -> None:
        with self.assertRaisesRegex(ValueError, "Unknown command"):
            self.engine.execute("missing", None, [])


if __name__ == "__main__":
    unittest.main()
