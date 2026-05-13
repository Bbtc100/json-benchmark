from __future__ import annotations

import unittest

from json_bench_python.core.commands import FilterCommand


class FilterCommandTest(unittest.TestCase):
    def setUp(self) -> None:
        self.command = FilterCommand()

    def test_identity_filter_returns_input(self) -> None:
        input_data = {"name": "abc", "nums": [1, 2, 3]}
        output = self.command.execute(input_data, ["."])
        self.assertEqual(input_data, output)

    def test_default_filter_is_identity_when_args_missing(self) -> None:
        input_data = {"x": 1}
        self.assertEqual(input_data, self.command.execute(input_data, None))
        self.assertEqual(input_data, self.command.execute(input_data, []))

    def test_can_read_nested_field(self) -> None:
        input_data = {"user": {"name": "abc"}}
        self.assertEqual("abc", self.command.execute(input_data, [".user.name"]))

    def test_can_iterate_array(self) -> None:
        input_data = {"nums": [1, 2, 3]}
        output = self.command.execute(input_data, [".nums.[]"])
        self.assertEqual([1, 2, 3], output)

    def test_can_filter_by_equals_predicate(self) -> None:
        input_data = {
            "users": [
                {"id": 0, "age": 75, "nested_0": {"value": 15}},
                {"id": 1, "age": 35, "nested_0": {"value": 99}},
                {"id": 2, "age": 35, "nested_0": {"value": 15}},
            ]
        }
        output = self.command.execute(input_data, [".users[?id==1]"])
        self.assertEqual([{"id": 1, "age": 35, "nested_0": {"value": 99}}], output)

    def test_can_filter_by_not_equals_predicate(self) -> None:
        input_data = {
            "users": [
                {"id": 0, "age": 75},
                {"id": 1, "age": 35},
                {"id": 2, "age": 35},
            ]
        }
        output = self.command.execute(input_data, [".users[?age!=35]"])
        self.assertEqual([{"id": 0, "age": 75}], output)

    def test_can_filter_by_greater_than_predicate(self) -> None:
        input_data = {
            "users": [
                {"id": 0, "age": 75},
                {"id": 1, "age": 35},
                {"id": 2, "age": 40},
            ]
        }
        output = self.command.execute(input_data, [".users[?age>39]"])
        self.assertEqual([{"id": 0, "age": 75}, {"id": 2, "age": 40}], output)

    def test_can_filter_by_less_than_predicate(self) -> None:
        input_data = {
            "users": [
                {"id": 0, "age": 75},
                {"id": 1, "age": 35},
                {"id": 2, "age": 40},
            ]
        }
        output = self.command.execute(input_data, [".users[?age<40]"])
        self.assertEqual([{"id": 1, "age": 35}], output)

    def test_supports_string_comparison_operators(self) -> None:
        input_data = {
            "users": [
                {"id": 0, "name": "anna"},
                {"id": 1, "name": "john"},
                {"id": 2, "name": "zoe"},
            ]
        }
        output = self.command.execute(input_data, [".users[?name>\"john\"]"])
        self.assertEqual([{"id": 2, "name": "zoe"}], output)

    def test_returns_empty_array_when_no_predicate_match(self) -> None:
        input_data = {"users": [{"id": 1}, {"id": 2}]}
        output = self.command.execute(input_data, [".users[?id==999]"])
        self.assertEqual([], output)

    def test_fails_when_filter_does_not_start_with_dot(self) -> None:
        input_data = {"name": "abc"}
        with self.assertRaisesRegex(ValueError, "must start with '\\.'"):
            self.command.execute(input_data, ["name"])

    def test_fails_when_trying_to_iterate_non_array(self) -> None:
        input_data = {"name": "abc"}
        with self.assertRaisesRegex(ValueError, "Cannot iterate over non-array"):
            self.command.execute(input_data, [".name.[]"])

    def test_fails_when_predicate_used_on_non_array_node(self) -> None:
        input_data = {"user": {"id": 1}}
        with self.assertRaisesRegex(ValueError, "Predicate filter applies to arrays only"):
            self.command.execute(input_data, [".user[?id==1]"])

    def test_fails_for_type_mismatch_in_greater_than(self) -> None:
        input_data = {"users": [{"id": 1, "name": "a"}]}
        with self.assertRaisesRegex(ValueError, "Cannot compare values with > operator"):
            self.command.execute(input_data, [".users[?name>5]"])

    def test_fails_for_unknown_predicate_operator(self) -> None:
        input_data = {"users": [{"id": 1}]}
        with self.assertRaisesRegex(ValueError, "must contain one of"):
            self.command.execute(input_data, [".users[?id>=1]"])


if __name__ == "__main__":
    unittest.main()
