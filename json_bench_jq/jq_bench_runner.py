import subprocess
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = Path(BASE_DIR / ".." / "data").resolve()

SMALL_FILES = [
    "data_10k.json",
    "data_100k.json",
    "data_1M.json",
    "data_10k_3.json",
    "data_100k_3.json",
    "data_1M_3.json",
]

LARGE_FILES = [
    "data_10M.json",
    "data_10M_3.json",
]

FILES = SMALL_FILES + LARGE_FILES

RESULTS_DIR = Path("jq_benchmark_results")
RESULTS_DIR.mkdir(exist_ok=True)

HYPERFINE = [
    "hyperfine",
    "--warmup", "5",
    "--runs", "5",
]


def run_benchmark(name: str, command: str):
    output_file = RESULTS_DIR / f"{name}.json"

    cmd = (
        HYPERFINE
        + [
            "--export-json",
            str(output_file),
            command,
        ]
    )

    print(f"Running benchmark: {name}")
    subprocess.run(cmd, check=True)


def benchmark_length(files):
    for file_name in files:
        file_path = DATA_DIR / file_name

        name = f"length_{Path(file_name).stem}"

        command = (
            f'jq ".users | length" "{file_path}" > nul'
        )

        run_benchmark(name, command)


def benchmark_map(files):
    map_ops = {
        "+100": (
            "walk(if type == \\\"number\\\" then . + 100 else . end)"
        ),
        "/4": (
            "walk(if type == \\\"number\\\" then . / 4 else . end)"
        ),
        "^6": (
            "walk(if type == \\\"number\\\" then (. * . * . * . * . * .) else . end)"
        ),
    }

    for file_name in files:
        file_path = DATA_DIR / file_name
        stem = Path(file_name).stem

        for op, jq_expr in map_ops.items():
            safe_op = (
                op.replace("+", "plus")
                   .replace("/", "div")
                   .replace("^", "pow")
            )

            name = f"map_{safe_op}_{stem}"

            command = (
                f'jq \"{jq_expr}\" "{file_path}" > nul'
            )

            run_benchmark(name, command)


def benchmark_filter(files):
    filter_fields = [
        "id",
        "age"
    ]

    for file_name in files:
        nested = "_3" in file_name
        file_path = DATA_DIR / file_name

        for field in filter_fields:

            if nested:
                pred_value = "5555" if field == "id" else "3012"
                operator = "==" if field == "id" else ">"

                expr = (
                    f'.users[] | '
                    f'select(.{field} {operator} {pred_value}) | '
                    '.nested_0.nested_1.nested_2'
                )

            else:
                pred_value = "100" if field == "id" else "4099"
                operator = "<" if field == "id" else "!="

                expr = (
                    f'.users[] | '
                    f'select(.{field} {operator} {pred_value})'
                )

            name = f"filter_{field}_{Path(file_name).stem}"

            command = (
                f'jq \"{expr}\" "{file_path}" > nul'
            )

            run_benchmark(name, command)


def run_small(fn):
    print("\n=== SMALL FILES ===")
    fn(SMALL_FILES)

def run_large(fn):
    print("\n=== LARGE FILES ===")
    fn(LARGE_FILES)


def main():

    run_small(benchmark_length)
    run_small(benchmark_map)
    run_small(benchmark_filter)

    run_large(benchmark_length)
    run_large(benchmark_map)
    run_large(benchmark_filter)

    print("\nAll jq benchmarks completed.")


if __name__ == "__main__":
    main()