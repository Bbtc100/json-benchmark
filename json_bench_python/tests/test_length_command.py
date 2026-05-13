from __future__ import annotations

import unittest

from json_bench_python.core.commands import LengthCommand


class LengthCommandTest(unittest.TestCase):
    def setUp(self) -> None:
        self.command = LengthCommand()

    def test_returns_zero_for_null_input(self) -> None:
        self.assertEqual(0, self.command.execute(None, None))

    def test_returns_array_length(self) -> None:
        self.assertEqual(4, self.command.execute([1, 2, 3, 4], None))

    def test_returns_object_field_count(self) -> None:
        self.assertEqual(3, self.command.execute({"a": 1, "b": 2, "c": 3}, None))

    def test_can_measure_filtered_value(self) -> None:
        self.assertEqual(3, self.command.execute({"users": [1, 2, 3], "meta": True}, [".users"]))

    def test_returns_string_length(self) -> None:
        self.assertEqual(5, self.command.execute("hello", None))

    def test_returns_binary_length(self) -> None:
        self.assertEqual(3, self.command.execute(bytes([1, 2, 3]), None))

    def test_returns_zero_for_number_and_boolean(self) -> None:
        self.assertEqual(0, self.command.execute(123, None))
        self.assertEqual(0, self.command.execute(True, None))


if __name__ == "__main__":
    unittest.main()
