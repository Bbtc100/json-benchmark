import json
import random
import string
import argparse


def random_string(n=10):
    return ''.join(random.choices(string.ascii_lowercase, k=n))


def generate_user(i):
    return {
        "id": i,
        "name": random_string(8),
        "age": random.randint(18, 80),
        "country": random.choice(["US", "DE", "AT", "HU", "UK"]),
        "email": f"user{i}@example.com"
    }


def generate_user_nested(i, depth):
    base = {
        "id": i,
        "age": random.randint(18, 80)
    }

    current = base
    for level in range(depth):
        current[f"nested_{level}"] = {
            "value": random.randint(0, 100),
            "text": random_string(5)
        }
        current = current[f"nested_{level}"]

    return base


def generate_file(filename, n, depth):
    with open(filename, "w") as f:
        f.write('{"users":[')

        for i in range(n):
            if depth <= 1:
                user = generate_user(i)
            else:
                user = generate_user_nested(i, depth)

            json.dump(user, f, separators=(',', ':'))

            if i < n - 1:
                f.write(',\n')

        f.write(']}')


def main():

    parser = argparse.ArgumentParser(description="Generate JSON benchmark files")
    parser.add_argument("depth", type=int, nargs="?", default=1,
                        help="Nesting depth (1 = no nesting)")

    args = parser.parse_args()

    depth = args.depth

    random.seed(42)

    # approx. size wihtout nesting:
    #   n == 100k: ~10MB
    #   n == 1M: ~100MB
    #   n == 10M: ~1GB
    
    sizes = {
        "10k": 10_000,
        "100k": 100_000,
        #"1M": 1_000_000,
        #"10M": 10_000_000
    }

    for label, count in sizes.items():
        filename = f"data_{label}.json"
        print(f"Generating {filename} with {count} records (depth={depth})...")
        generate_file(filename, count, depth)

    print("Done.")


if __name__ == "__main__":
    main()