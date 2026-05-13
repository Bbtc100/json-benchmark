import json
import random
import string
import argparse
import multiprocessing as mp
from pathlib import Path

LETTERS = string.ascii_lowercase
COUNTRIES = ("US", "DE", "AT", "HU", "UK")

# Tune this depending on RAM / CPU
CHUNK_SIZE = 100_000


def random_string(rng, n=10):
    return ''.join(rng.choice(LETTERS) for _ in range(n))


def generate_user(rng, i):
    return (
        f'{{"id":{i},'
        f'"name":"{random_string(rng, 8)}",'
        f'"age":{rng.randint(18,80)},'
        f'"country":"{rng.choice(COUNTRIES)}",'
        f'"email":"user{i}@example.com"}}'
    )


def generate_user_nested(rng, i, depth):
    result = [
        f'{{"id":{i},"age":{rng.randint(18,80)}'
    ]

    for level in range(depth):
        result.append(
            f',"nested_{level}":{{'
            f'"value":{rng.randint(0,100)},'
            f'"text":"{random_string(rng, 5)}"'
        )

    result.append("}" * depth)
    result.append("}")

    return ''.join(result)


def worker(args):
    start, end, depth, seed, temp_path = args

    rng = random.Random(seed)

    with open(temp_path, "w", buffering=1024 * 1024) as f:

        first = True

        for i in range(start, end):

            if depth <= 1:
                s = generate_user(rng, i)
            else:
                s = generate_user_nested(rng, i, depth)

            if not first:
                f.write(',')

            f.write(s)

            first = False

    return temp_path


def generate_file(filename, n, depth):

    cpu_count = mp.cpu_count()

    ranges = []

    temp_files = []

    for idx, start in enumerate(range(0, n, CHUNK_SIZE)):

        end = min(start + CHUNK_SIZE, n)

        temp_path = f"{filename}.part{idx}"

        temp_files.append(temp_path)

        ranges.append((
            start,
            end,
            depth,
            42 + idx,   # deterministic but unique per process
            temp_path
        ))

    workers = min(cpu_count, len(ranges))

    with mp.Pool(workers) as pool:
        pool.map(worker, ranges)

    with open(filename, "w", buffering=1024 * 1024) as out:

        out.write('{"users":[')

        first = True

        for temp_file in temp_files:

            with open(temp_file, "r", buffering=1024 * 1024) as f:

                if not first:
                    out.write(',')

                out.write(f.read())

            Path(temp_file).unlink()

            first = False

        out.write(']}')


def main():

    parser = argparse.ArgumentParser(
        description="Generate JSON benchmark files"
    )

    parser.add_argument(
        "depth",
        type=int,
        nargs="?",
        default=1,
        help="Nesting depth (1 = no nesting)"
    )

    args = parser.parse_args()

    depth = args.depth

    sizes = {
        "10k": 10_000,
        "100k": 100_000,
        "1M": 1_000_000,
        "10M": 10_000_000
    }

    for label, count in sizes.items():

        if depth <= 1:
            filename = f"data_{label}.json"
        else:
            filename = f"data_{label}_{depth}.json"

        print(
            f"Generating {filename} "
            f"with {count} records (depth={depth})..."
        )

        generate_file(filename, count, depth)

    print("Done.")


if __name__ == "__main__":
    mp.freeze_support()  # Windows support
    main()