"""Executable module for json_bench_python CLI."""

from __future__ import annotations

import sys

from json_bench_python.cli import main


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
