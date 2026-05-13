"""jq-like filter command implementation."""

from __future__ import annotations

import json
from dataclasses import dataclass
from enum import Enum
from typing import Any, Sequence


class Operator(Enum):
    EQUALS = "=="
    NOT_EQUALS = "!="
    GREATER_THAN = ">"
    LESS_THAN = "<"


class SegmentType(Enum):
    IDENTITY = "IDENTITY"
    FIELD = "FIELD"
    ITERATE = "ITERATE"
    FILTER = "FILTER"


@dataclass(frozen=True)
class ParsedPredicate:
    field_path: str
    operator: Operator
    literal: str


@dataclass(frozen=True)
class PathSegment:
    type: SegmentType
    value: str | None


class FilterCommand:
    """Applies path and predicate filters to Python JSON values."""

    def name(self) -> str:
        return "filter"

    def execute(self, input_data: Any, args: Sequence[str] | None) -> Any:
        filter_expr = "." if not args else args[0]
        return self._apply_filter(input_data, filter_expr)

    def _apply_filter(self, node: Any, filter_expr: str) -> Any:
        segments = self._parse_filter(filter_expr)
        return self._evaluate(node, segments)

    def _parse_filter(self, expr: str) -> list[PathSegment]:
        segments: list[PathSegment] = []

        if expr == ".":
            segments.append(PathSegment(SegmentType.IDENTITY, None))
            return segments

        if not expr.startswith("."):
            raise ValueError(f"Filter must start with '.' (got: {expr})")

        parts = expr[1:].split(".")
        for part in parts:
            if not part:
                continue

            predicate_start = part.find("[?")
            if predicate_start >= 0:
                field = part[:predicate_start]
                if field:
                    segments.append(PathSegment(SegmentType.FIELD, field))

                predicate_end = part.find("]", predicate_start + 1)
                if predicate_end < 0:
                    raise ValueError(f"Unterminated expression in: {expr}")

                predicate = part[predicate_start + 2 : predicate_end]
                if not predicate:
                    raise ValueError(f"Empty predicate in: {expr}")

                segments.append(PathSegment(SegmentType.FILTER, predicate))

                tail = part[predicate_end + 1 :]
                if tail:
                    raise ValueError(f"Unexpected characters after predicate in: {expr}")
            elif part == "[]":
                segments.append(PathSegment(SegmentType.ITERATE, None))
            else:
                segments.append(PathSegment(SegmentType.FIELD, part))

        return segments

    def _evaluate(self, node: Any, segments: list[PathSegment]) -> Any:
        if not segments:
            return node

        current = segments[0]
        rest = segments[1:]

        if current.type == SegmentType.IDENTITY:
            return node if not rest else self._evaluate(node, rest)

        if current.type == SegmentType.FIELD:
            if not isinstance(node, dict):
                return None

            field = node.get(current.value)
            if field is None:
                return None

            return field if not rest else self._evaluate(field, rest)

        if current.type == SegmentType.ITERATE:
            if not isinstance(node, list):
                raise ValueError(f"Cannot iterate over non-array type: {json_type_name(node)}")

            result: list[Any] = []
            for item in node:
                evaluated = item if not rest else self._evaluate(item, rest)
                result.append(evaluated)
            return result

        if current.type == SegmentType.FILTER:
            if not isinstance(node, list):
                raise ValueError(
                    f"Predicate filter applies to arrays only, found: {json_type_name(node)}"
                )

            filtered: list[Any] = []
            for item in node:
                if self._matches_predicate(item, current.value or ""):
                    evaluated = item if not rest else self._evaluate(item, rest)
                    filtered.append(evaluated)
            return filtered

        raise ValueError(f"Unknown path segment type: {current.type}")

    def _matches_predicate(self, item: Any, pred: str) -> bool:
        predicate = self._parse_predicate(pred)

        target = item.get(predicate.field_path) if isinstance(item, dict) else None
        if target is None:
            return False

        literal = self._parse_literal(predicate.literal)
        return self._compare(target, literal, predicate.operator)

    def _parse_predicate(self, raw: str) -> ParsedPredicate:
        trimmed = raw.strip()
        if ">=" in trimmed or "<=" in trimmed:
            raise ValueError(
                "Predicate must contain one of the following operators: (==, !=, >, <): "
                f"{raw}"
            )

        for symbol, operator in (
            ("==", Operator.EQUALS),
            ("!=", Operator.NOT_EQUALS),
            (">", Operator.GREATER_THAN),
            ("<", Operator.LESS_THAN),
        ):
            idx = trimmed.find(symbol)
            if idx >= 0:
                return self._split_predicate(trimmed, idx, operator)

        raise ValueError(
            "Predicate must contain one of the following operators: (==, !=, >, <): "
            f"{raw}"
        )

    def _split_predicate(self, text: str, op_idx: int, operator: Operator) -> ParsedPredicate:
        op_len = len(operator.value)
        left = text[:op_idx].strip()
        right = text[op_idx + op_len :].strip()

        if "." in left:
            raise ValueError(f"Predicate path must be a single field: {left}")

        if not left or not right:
            raise ValueError(f"Illegal predicate: {text}")

        return ParsedPredicate(left, operator, right)

    def _parse_literal(self, raw: str) -> Any:
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return raw

    def _compare(self, left: Any, right: Any, operator: Operator) -> bool:
        if operator == Operator.EQUALS:
            return left == right

        if operator == Operator.NOT_EQUALS:
            return left != right

        if operator == Operator.GREATER_THAN:
            return self._greater_than(left, right)

        return self._less_than(left, right)

    def _greater_than(self, left: Any, right: Any) -> bool:
        if is_json_number(left) and is_json_number(right):
            return left > right

        if isinstance(left, str) and isinstance(right, str):
            return left > right

        raise ValueError(f"Cannot compare values with > operator: {left} and {right}")

    def _less_than(self, left: Any, right: Any) -> bool:
        if is_json_number(left) and is_json_number(right):
            return left < right

        if isinstance(left, str) and isinstance(right, str):
            return left < right

        raise ValueError(f"Cannot compare values with < operator: {left} and {right}")


def is_json_number(value: Any) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def json_type_name(value: Any) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, dict):
        return "object"
    if isinstance(value, list):
        return "array"
    if isinstance(value, str):
        return "string"
    if is_json_number(value):
        return "number"
    return type(value).__name__
