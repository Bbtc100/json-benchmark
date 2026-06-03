from __future__ import annotations

import csv
import json
import statistics
from pathlib import Path


RESULTS_DIR = Path("results")
OUTPUT_CSV = Path("benchmark_summary.csv")


def detect_tool(command: str) -> str:

    cmd = command.lower()

    if 'java -jar' in cmd:
        if '"single"' in cmd:
            return "java_single"
        if '"multi"' in cmd:
            return "java_multi"

    if cmd.startswith("python "):
        return "python"

    if cmd.startswith("jq "):
        return "jq"

    return "unknown"

def parse_filename(stem: str):

    parts = stem.split("_")

    if parts[0] == "length":
        operation = "length"
        dataset = "_".join(parts[1:])

    elif parts[0] == "filter":
        operation = f"filter_{parts[1]}"
        dataset = "_".join(parts[2:])

    elif parts[0] == "map":
        operation = f"map_{parts[1]}"
        dataset = "_".join(parts[2:])

    else:
        raise ValueError(f"Unknown benchmark file: {stem}")

    variant = (
        "nested"
        if dataset.endswith("_3")
        else "normal"
    )

    return operation, dataset, variant


def load_results(rows):

    for file in RESULTS_DIR.glob("*.json"):

        operation, dataset, variant = parse_filename(file.stem)
        
        with open(file, encoding="utf-8") as f:
            data = json.load(f)

        for result in data.get("results", []):

            tool = detect_tool(result["command"])

            add_row(
                rows,
                tool,
                operation,
                dataset,
                variant,
                result["mean"] * 1000,
                result["stddev"] * 1000,
            )

def parse_dataset(stem: str) -> tuple[str, str]:
    if stem.endswith("_3"):
        return stem.replace("_3", ""), "nested"

    return stem, "normal"


def add_row(rows, tool, operation, dataset, variant, mean_ms, stddev_ms):
    rows.append(
        {
            "tool": tool,
            "operation": operation,
            "dataset": dataset,
            "variant": variant,
            "mean_ms": mean_ms,
            "stddev_ms": stddev_ms,
        }
    )

def save_csv(rows):

    fieldnames = [
        "tool",
        "operation",
        "dataset",
        "variant",
        "mean_ms",
        "stddev_ms",
    ]

    with open(
        OUTPUT_CSV,
        "w",
        newline="",
        encoding="utf-8",
    ) as f:

        writer = csv.DictWriter(
            f,
            fieldnames=fieldnames,
        )

        writer.writeheader()

        writer.writerows(rows)


def main():

    rows = []

    load_results(rows)

    save_csv(rows)

    print(f"Saved {len(rows)} rows to {OUTPUT_CSV}")


if __name__ == "__main__":
    main()