from __future__ import annotations

import csv
import json
import statistics
from pathlib import Path


JMH_FILE = Path("java_benchmark_results/java_benchmark_results.json")
PYPERF_DIR = Path("python_benchmark_results")
JQ_DIR = Path("jq_benchmark_results")

OUTPUT_CSV = Path("benchmark_summary.csv")


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


def load_jmh(rows):
    with open(JMH_FILE, encoding="utf-8") as f:
        data = json.load(f)

    for benchmark in data:

        benchmark_name = benchmark["benchmark"].split(".")[-1]

        params = benchmark["params"]

        file_name = params["fileName"]
        stem = Path(file_name).stem

        dataset, variant = parse_dataset(stem)

        operation = benchmark_name

        mean_ms = benchmark["primaryMetric"]["score"]

        stddev_ms = benchmark["primaryMetric"]["scoreError"]

        add_row(
            rows,
            "java",
            operation,
            dataset,
            variant,
            mean_ms,
            stddev_ms,
        )


def load_pyperf(rows):

    for file in PYPERF_DIR.glob("*.json"):

        stem = file.stem

        parts = stem.split("_")

        if parts[0] == "length":
            operation = "length"
            dataset_stem = "_".join(parts[1:])

        elif parts[0] == "filter":
            operation = f"filter_{parts[1]}"
            dataset_stem = "_".join(parts[2:])

        elif parts[0] == "map":
            operation = f"map_{parts[1]}"
            dataset_stem = "_".join(parts[2:])

        else:
            continue

        dataset, variant = parse_dataset(dataset_stem)

        with open(file, encoding="utf-8") as f:
            data = json.load(f)

        values = []

        for run in data["benchmarks"][0]["runs"]:

            values.extend(run.get("values", []))

        if not values:
            continue

        mean_ms = statistics.mean(values) * 1000

        stddev_ms = (
            statistics.stdev(values) * 1000
            if len(values) > 1
            else 0.0
        )

        add_row(
            rows,
            "python",
            operation,
            dataset,
            variant,
            mean_ms,
            stddev_ms,
        )


def load_hyperfine(rows):

    for file in JQ_DIR.glob("*.json"):

        stem = file.stem

        parts = stem.split("_")

        if parts[0] == "length":
            operation = "length"
            dataset_stem = "_".join(parts[1:])

        elif parts[0] == "filter":
            operation = f"filter_{parts[1]}"
            dataset_stem = "_".join(parts[2:])

        elif parts[0] == "map":
            operation = f"map_{parts[1]}"
            dataset_stem = "_".join(parts[2:])

        else:
            continue

        dataset, variant = parse_dataset(dataset_stem)

        with open(file, encoding="utf-8") as f:
            data = json.load(f)

        result = data["results"][0]

        mean_ms = result["mean"] * 1000

        stddev_ms = result["stddev"] * 1000

        add_row(
            rows,
            "jq",
            operation,
            dataset,
            variant,
            mean_ms,
            stddev_ms,
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

    load_jmh(rows)
    load_pyperf(rows)
    load_hyperfine(rows)

    save_csv(rows)

    print(f"Saved {len(rows)} rows to {OUTPUT_CSV}")


if __name__ == "__main__":
    main()