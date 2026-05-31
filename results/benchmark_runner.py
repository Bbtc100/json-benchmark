import subprocess
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent

DATA_DIR = (BASE_DIR / ".." / "data").resolve()
RESULTS_DIR = (BASE_DIR / "results").resolve()

RESULTS_DIR.mkdir(exist_ok=True)

JAVA_JAR = (
    BASE_DIR
    / ".."
    / "json_bench_java"
    / "target"
    / "json_bench_java-1.0-SNAPSHOT.jar"
).resolve()

PYTHON_MAIN = (
    BASE_DIR
    / ".."
    / "json_bench_python"
    / "main.py"
).resolve()

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

HYPERFINE_COMMON = [
    "hyperfine",
    "--warmup", "3",
    "--runs", "5",
]


def run_benchmark(name: str, commands: list[str]):

    output_file = RESULTS_DIR / f"{name}.json"

    cmd = (
        HYPERFINE_COMMON
        + [
            "--export-json",
            str(output_file),
        ]
        + commands
    )

    print(f"Running benchmark: {name}")

    subprocess.run(cmd, check=True)


def build_length_commands(file_path: Path):

    return [
        (
            f'java -jar "{JAVA_JAR}" '
            f'"single" "length" "{file_path}" ".users" '
            f'> nul'
        ),
        (
            f'java -jar "{JAVA_JAR}" '
            f'"multi" "length" "{file_path}" ".users" '
            f'> nul'
        ),
        (
            f'python "{PYTHON_MAIN}" '
            f'"length" "{file_path}" ".users" '
            f'> nul'
        ),
        (
            f'jq ".users | length" "{file_path}" '
            f'> nul'
        ),
    ]


def build_filter_id_commands(file_path: Path, nested: bool):

    expr = (
        ".users[?id==5555].nested_0.nested_1.nested_2"
        if nested
        else ".users[?id==5555]"
    )

    jq_expr = (
        '.users[] | '
        'select(.id == 5555) | '
        '.nested_0.nested_1.nested_2'
        if nested
        else
        '.users[] | select(.id == 5555)'
    )

    return [
        (
            f'java -jar "{JAVA_JAR}" '
            f'"single" "filter" "{file_path}" "{expr}" '
            f'> nul'
        ),
        (
            f'java -jar "{JAVA_JAR}" '
            f'"multi" "filter" "{file_path}" "{expr}" '
            f'> nul'
        ),
        (
            f'python "{PYTHON_MAIN}" '
            f'"filter" "{file_path}" "{expr}" '
            f'> nul'
        ),
        (
            f'jq "{jq_expr}" "{file_path}" '
            f'> nul'
        ),
    ]


def build_filter_age_commands(file_path: Path, nested: bool):

    expr = (
        ".users[?age<23].nested_0.nested_1.nested_2"
        if nested
        else ".users[?age<23]"
    )

    jq_expr = (
        '.users[] | '
        'select(.age < 23) | '
        '.nested_0.nested_1.nested_2'
        if nested
        else
        '.users[] | select(.age < 23)'
    )

    return [
        (
            f'java -jar "{JAVA_JAR}" '
            f'"single" "filter" "{file_path}" "{expr}" '
            f'> nul'
        ),
        (
            f'java -jar "{JAVA_JAR}" '
            f'"multi" "filter" "{file_path}" "{expr}" '
            f'> nul'
        ),
        (
            f'python "{PYTHON_MAIN}" '
            f'"filter" "{file_path}" "{expr}" '
            f'> nul'
        ),
        (
            f'jq "{jq_expr}" "{file_path}" '
            f'> nul'
        ),
    ]


def build_map_commands(file_path: Path):

    map_expr = ".^6"

    jq_expr = (
        'walk(if type == \\"number\\" '
        'then (. * . * . * . * . * .) '
        'else . end)'
    )

    return [
        (
            f'java -jar "{JAVA_JAR}" '
            f'"single" "map" "{file_path}" "{map_expr}" '
            f'> nul'
        ),
        (
            f'java -jar "{JAVA_JAR}" '
            f'"multi" "map" "{file_path}" "{map_expr}" '
            f'> nul'
        ),
        (
            f'python "{PYTHON_MAIN}" '
            f'"map" "{file_path}" "{map_expr}" '
            f'> nul'
        ),
        (
            f'jq "{jq_expr}" "{file_path}" '
            f'> nul'
        ),
    ]


def benchmark_files(files):

    for file_name in files:

        file_path = DATA_DIR / file_name
        stem = Path(file_name).stem

        nested = stem.endswith("_3")

        run_benchmark(
            f"length_{stem}",
            build_length_commands(file_path)
        )

        run_benchmark(
            f"filter_id_{stem}",
            build_filter_id_commands(file_path, nested)
        )

        run_benchmark(
            f"filter_age_{stem}",
            build_filter_age_commands(file_path, nested)
        )

        run_benchmark(
            f"map_pow6_{stem}",
            build_map_commands(file_path)
        )


def main():

    print("\n=== SMALL FILES ===")
    benchmark_files(SMALL_FILES)

    print("\n=== LARGE FILES ===")
    benchmark_files(LARGE_FILES)

    print("\nAll benchmarks completed.")


if __name__ == "__main__":
    main()