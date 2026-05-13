"""length command implementation."""

from __future__ import annotations

from typing import Any, Sequence

from .filter_command import FilterCommand


class LengthCommand:
    """Computes length for filtered JSON values."""

    def __init__(self) -> None:
        self._filter_command = FilterCommand()

    def name(self) -> str:
        return "length"

    def execute(self, input_data: Any, args: Sequence[str] | None) -> int:
        if args:
            if len(args) > 1:
                raise ValueError("length accepts at most one optional filter expression")
            input_data = self._filter_command.execute(input_data, [args[0]])

        if input_data is None:
            return 0

        if isinstance(input_data, (list, dict, str, bytes, bytearray)):
            return len(input_data)

        return 0
