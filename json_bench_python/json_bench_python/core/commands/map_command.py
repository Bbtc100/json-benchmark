"""map_values-style command implementation."""

from __future__ import annotations

import math
import re
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from typing import Any, Sequence

from .filter_command import FilterCommand, json_type_name


ARITHMETIC_EXPR = re.compile(r"^\s*\.?\s*([+\-*/^])\s*([+-]?(?:\d+(?:\.\d+)?|\.\d+))\s*$")
DIVISION_SCALE = Decimal("1." + ("0" * 16))


@dataclass(frozen=True)
class ArithmeticOp:
    operator: str
    operand: Decimal


class MapCommand:
    """Applies arithmetic transforms or filter expressions per value/element."""

    def __init__(self) -> None:
        self._filter_command = FilterCommand()

    def name(self) -> str:
        return "map"

    def execute(self, input_data: Any, args: Sequence[str] | None) -> Any:
        if input_data is None:
            return None

        filter_expr = "." if not args else args[0]

        if isinstance(input_data, dict):
            return {key: self._apply_expression(value, filter_expr) for key, value in input_data.items()}

        if isinstance(input_data, list):
            return [self._apply_expression(item, filter_expr) for item in input_data]

        raise ValueError(f"map_values expects object or array input, got: {json_type_name(input_data)}")

    def _apply_expression(self, node: Any, filter_expr: str) -> Any:
        arithmetic_op = self._parse_arithmetic(filter_expr)
        if arithmetic_op is not None:
            return self._apply_arithmetic_recursively(node, arithmetic_op)

        return self._filter_command.execute(node, [filter_expr])

    def _parse_arithmetic(self, expr: str) -> ArithmeticOp | None:
        matcher = ARITHMETIC_EXPR.match(expr)
        if not matcher:
            return None

        operator = matcher.group(1)
        operand = Decimal(matcher.group(2))

        if operator == "/" and operand == 0:
            raise ValueError(f"Division by zero is not allowed in map expression: {expr}")

        return ArithmeticOp(operator, operand)

    def _apply_arithmetic_recursively(self, node: Any, op: ArithmeticOp) -> Any:
        if node is None:
            return None

        if isinstance(node, dict):
            return {key: self._apply_arithmetic_recursively(value, op) for key, value in node.items()}

        if isinstance(node, list):
            return [self._apply_arithmetic_recursively(item, op) for item in node]

        if not is_json_number(node):
            return node

        value = Decimal(str(node))
        calculated = self._apply_operation(value, op)
        return as_json_number(calculated)

    def _apply_operation(self, value: Decimal, op: ArithmeticOp) -> Decimal:
        if op.operator == "+":
            return value + op.operand
        if op.operator == "-":
            return value - op.operand
        if op.operator == "*":
            return value * op.operand
        if op.operator == "/":
            return (value / op.operand).quantize(DIVISION_SCALE, rounding=ROUND_HALF_UP)
        if op.operator == "^":
            return pow_decimal(value, op.operand)

        raise ValueError(f"Unsupported operator: {op.operator}")


def is_json_number(value: Any) -> bool:
    return isinstance(value, (int, float, Decimal)) and not isinstance(value, bool)


def as_json_number(value: Decimal) -> int | float:
    normalized = value.normalize()

    if normalized == normalized.to_integral_value():
        return int(normalized)

    return float(normalized)


def pow_decimal(base: Decimal, exponent: Decimal) -> Decimal:
    normalized_exponent = exponent.normalize()

    if normalized_exponent == normalized_exponent.to_integral_value():
        int_exponent = int(normalized_exponent)

        if int_exponent >= 0:
            return base ** int_exponent

        if base == 0:
            raise ValueError("0 cannot be raised to a negative power.")

        result = Decimal(1) / (base ** abs(int_exponent))
        return result.quantize(DIVISION_SCALE, rounding=ROUND_HALF_UP)

    if base < 0:
        raise ValueError("Negative base with non-integer exponent is not a real number.")

    result = math.pow(float(base), float(exponent))
    if math.isnan(result) or math.isinf(result):
        raise ValueError("Power operation produced a non-finite result.")

    try:
        return Decimal(str(result))
    except InvalidOperation as exc:
        raise ValueError("Power operation produced an invalid numeric result.") from exc
