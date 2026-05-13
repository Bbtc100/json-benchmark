"""Command protocol for JSON processor commands."""

from __future__ import annotations

from typing import Any, Protocol, Sequence


class Command(Protocol):
    """Contract for JSON processor commands."""

    def name(self) -> str:
        """Return command name used in CLI dispatch."""

    def execute(self, input_data: Any, args: Sequence[str] | None) -> Any:
        """Execute command on parsed JSON input with optional arguments."""
