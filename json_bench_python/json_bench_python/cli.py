"""CLI entrypoint and orchestration for JSON benchmark processor."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Sequence

from .engine import SingleEngine


ENGINE = SingleEngine()


def main(argv: Sequence[str] | None = None) -> int:
    args = list(argv) if argv is not None else []

    if not args:
        print("No command provided.")
        print("Use 'help' or 'h' to see available commands.")
        return 0

    first_arg = args[0]
    if first_arg in {"help", "h"}:
        print_help()
        return 0

    if len(args) < 2:
        print("Missing input file.")
        print("Use 'help' or 'h' for usage details.")
        return 0

    command_name = first_arg
    input_path = Path(args[1])
    command_args = args[2:]

    if not input_path.exists():
        print(f"Input file not found: {input_path}")
        return 0

    try:
        input_data = json.loads(input_path.read_text(encoding="utf-8"))
        output = ENGINE.execute(command_name, input_data, command_args)
        print(json.dumps(output, separators=(",", ":"), ensure_ascii=False))

    except ValueError as exc:
        if str(exc).startswith("Unknown command:"):
            available = ", ".join(sorted(ENGINE.command_names()))
            print(str(exc))
            print(f"Available commands: {available}")
            print("Use 'help' or 'h' to see full command documentation.")
            return 0

        print(f"Command failed: {exc}")

    except json.JSONDecodeError as exc:
        print(f"Failed to read JSON from file: {input_path}")
        print(str(exc))

    except OSError as exc:
        print(f"Failed to read JSON from file: {input_path}")
        print(str(exc))

    return 0


def print_help() -> None:
    print("Usage: <command> <file> [command-args...]")
    print()
    print("Commands:")
    print("  length <file> [expr]")
    print("    Returns the length of the JSON input.")
    print("    If expr is provided, it is applied as a filter first.")
    print("    Arrays/objects -> element count, strings -> character count, others -> 0.")
    print()
    print("  filter <file> [expr]")
    print("    Applies a simple jq-like path filter expression.")
    print("    Default expression is '.' (identity).")
    print("    Path examples: . , .name , .items.[]")
    print("    Predicate examples:")
    print("      .users[?id==0]")
    print("      .users[?age>40]")
    print("    Supported predicate operators: ==, !=, >, <")
    print("    Predicate field path is a single field name (no dotted nested path inside predicate).")
    print()
    print("  map <file> [expr]")
    print("    Applies map_values-like transformation to each object value or array element.")
    print("    Supports arithmetic expressions like +1, -2, *3, /4, ^0.5.")
    print("    You may also pass a filter expression (e.g. .name). Default is '.'.")
    print()
    print("  help | h")
    print("    Shows this message.")
