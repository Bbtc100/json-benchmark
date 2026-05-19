from __future__ import annotations

from typing import Any

from .engine import SingleEngine


ENGINE = SingleEngine()


def length(data: Any, expr: str = ".") -> Any:
    return ENGINE.execute("length", data, [expr])


def map_values(data: Any, expr: str) -> Any:
    return ENGINE.execute("map", data, [expr])


def filter_data(data: Any, expr: str) -> Any:
    return ENGINE.execute("filter", data, [expr])