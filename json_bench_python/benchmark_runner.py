import json
import subprocess
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = Path(BASE_DIR / ".." / "data").resolve()

FILES = [
    "data_10k.json",
    "data_100k.json",
    "data_1M.json",
    "data_10M.json",
    "data_10k_3.json",
    "data_100k_3.json",
    "data_1M_3.json",
    "data_10M_3.json"
]

RESULTS_DIR = Path("python_benchmark_results")
RESULTS_DIR.mkdir(exist_ok=True)

PYTHON = "python"
PYPERF = [PYTHON, "-m", "pyperf", "command"]

COMMON_ARGS = [
    "--rigorous",
    "--duplicate", "3",
    "--warmups", "5"
]


def run_benchmark(name: str, command: list[str]):
    output_file = RESULTS_DIR / f"{name}.json"

    cmd = (
        PYPERF
        + COMMON_ARGS
        + ["--output", str(output_file), "--"]
        + command
    )

    print(f"Running benchmark: {name}")
    subprocess.run(cmd, check=True)


def benchmark_length():
    for file_name in FILES:
        name = f"length_{Path(file_name).stem}"

        cmd = [
            PYTHON,
            "main.py",
            "length",
            str(DATA_DIR / file_name),
            ".users"
        ]

        run_benchmark(name, cmd)


def benchmark_map():
    map_ops = [
        "+100",
        "/4",
        "^6"
    ]

    for file_name in FILES:
        for op in map_ops:
            safe_op = (
                op.replace("+", "plus")
                  .replace("/", "div")
                  .replace("^", "pow")
            )

            name = f"map_{safe_op}_{Path(file_name).stem}"

            cmd = [
                PYTHON,
                "main.py",
                "map",
                str(DATA_DIR / file_name),
                op
            ]

            run_benchmark(name, cmd)


def benchmark_filter():
    filter_fields = [
        "id",
        "age"
    ]

    for file_name in FILES:
        nested = "_3" in file_name

        for field in filter_fields:

            if nested:
                pred_value = "5555" if field == "id" else "3012"
                operator = "==" if field == "id" else ">"

                expr = (
                    f".users[?{field}{operator}{pred_value}]"
                    ".nested_0.nested_1.nested_2"
                )
            else:
                pred_value = "100" if field == "id" else "4099"
                operator = "<" if field == "id" else "!="

                expr = f".users[?{field}{operator}{pred_value}]"

            name = f"filter_{field}_{Path(file_name).stem}"

            cmd = [
                PYTHON,
                "main.py",
                "filter",
                str(DATA_DIR / file_name),
                expr
            ]

            run_benchmark(name, cmd)


def main():
    benchmark_length()
    benchmark_map()
    benchmark_filter()

    print("\nAll benchmarks completed.")


if __name__ == "__main__":
    main()