from __future__ import annotations

import unittest

from json_bench_python.core.commands import MapCommand


class MapCommandTest(unittest.TestCase):
    def setUp(self) -> None:
        self.command = MapCommand()

    def test_maps_object_values_recursively_with_arithmetic(self) -> None:
        input_data = {"name": "abc", "nums": [1, 2, 3, 4]}
        output = self.command.execute(input_data, ["+1"])
        self.assertEqual({"name": "abc", "nums": [2, 3, 4, 5]}, output)

    def test_supports_optional_leading_dot_in_arithmetic_expression(self) -> None:
        input_data = {"nums": [1, 2, 3]}
        output = self.command.execute(input_data, [".+1"])
        self.assertEqual({"nums": [2, 3, 4]}, output)

    def test_supports_power_with_non_integer_exponent(self) -> None:
        input_data = [1, 4, 9]
        output = self.command.execute(input_data, ["^0.5"])
        self.assertEqual([1, 2, 3], output)

    def test_can_apply_filter_expression_to_array_elements(self) -> None:
        input_data = [{"name": "a"}, {"name": "b"}]
        output = self.command.execute(input_data, [".name"])
        self.assertEqual(["a", "b"], output)

    def test_rejects_division_by_zero(self) -> None:
        input_data = [1, 2, 3]
        with self.assertRaisesRegex(ValueError, "Division by zero"):
            self.command.execute(input_data, ["/0"])

    def test_rejects_primitive_root_input(self) -> None:
        input_data = 123
        with self.assertRaisesRegex(ValueError, "expects object or array input"):
            self.command.execute(input_data, ["+1"])

    def test_returns_null_for_null_input(self) -> None:
        self.assertIsNone(self.command.execute(None, ["+1"]))

    def test_rejects_invalid_filter_expression_without_dot(self) -> None:
        input_data = [{"name": "a"}]
        with self.assertRaisesRegex(ValueError, "must start with '\\.'"):
            self.command.execute(input_data, ["name"])


if __name__ == "__main__":
    unittest.main()
