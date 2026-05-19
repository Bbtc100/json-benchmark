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
PYPERF = [PYTHON, "-m", "pyperf", "timeit"]

COMMON_ARGS = [
    "--rigorous"
]


def run_benchmark(name: str, setup: str, statement: str):
    output_file = RESULTS_DIR / f"{name}.json"

    cmd = (
        PYPERF
        + COMMON_ARGS
        + [
            "--setup",
            setup,
            "--output",
            str(output_file),
            statement,
        ]
    )

    print(f"Running benchmark: {name}")
    subprocess.run(cmd, check=True)


def benchmark_length():
    for file_name in FILES:
        name = f"length_{Path(file_name).stem}"

        file_path = DATA_DIR / file_name

        setup = f"""
import json
from json_bench_python.benchmark_api import length

with open(r"{file_path}", "r", encoding="utf-8") as f:
    data = json.load(f)
"""

        statement = 'result = length(data, ".users")'

        run_benchmark(name, setup, statement)


def benchmark_map():
    map_ops = [
        "+100",
        "/4",
        "^6"
    ]

    for file_name in FILES:
        file_path = DATA_DIR / file_name

        for op in map_ops:
            safe_op = (
                op.replace("+", "plus")
                  .replace("/", "div")
                  .replace("^", "pow")
            )

            name = f"map_{safe_op}_{Path(file_name).stem}"

            setup = f"""
import json
from json_bench_python.benchmark_api import map_values

with open(r"{file_path}", "r", encoding="utf-8") as f:
    data = json.load(f)

op = "{op}"
"""

            statement = "result = map_values(data, op)"

            run_benchmark(name, setup, statement)


def benchmark_filter():
    filter_fields = [
        "id",
        "age"
    ]

    for file_name in FILES:
        nested = "_3" in file_name
        file_path = DATA_DIR / file_name

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

            setup = f"""
import json
from json_bench_python.benchmark_api import filter_data

with open(r"{file_path}", "r", encoding="utf-8") as f:
    data = json.load(f)

expr = "{expr}"
"""

            statement = "result = filter_data(data, expr)"

            run_benchmark(name, setup, statement)


def main():
    benchmark_length()
    benchmark_map()
    benchmark_filter()

    print("\nAll benchmarks completed.")


if __name__ == "__main__":
    main()