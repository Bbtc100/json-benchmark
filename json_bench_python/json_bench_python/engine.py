"""Single-thread command dispatch engine."""

from __future__ import annotations

from typing import Any, Sequence

from .core.command import Command
from .core.commands import FilterCommand, LengthCommand, MapCommand


class SingleEngine:
    """Single-threaded command registry and executor."""

    def __init__(self) -> None:
        self._commands: dict[str, Command] = {}
        self._register(LengthCommand())
        self._register(FilterCommand())
        self._register(MapCommand())

    def name(self) -> str:
        return "single"

    def command_names(self) -> set[str]:
        return set(self._commands.keys())

    def execute(self, command_name: str, input_data: Any, args: Sequence[str] | None) -> Any:
        command = self._commands.get(command_name)
        if command is None:
            raise ValueError(f"Unknown command: {command_name}")

        safe_args = [] if args is None else list(args)
        return command.execute(input_data, safe_args)

    def _register(self, command: Command) -> None:
        self._commands[command.name()] = command
