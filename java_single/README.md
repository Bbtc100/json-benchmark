# java_single JSON benchmark CLI

A small single-threaded Java JSON processor with jq-like commands:

- `length`
- `filter`
- `map` (currently map_values style)
- `help` / `h`

Current suite covers CLI behavior (`Main`) and all command classes (`LengthCommand`, `FilterCommand`, `MapCommand`) including error and edge cases.

## Usage in CLI
From the project root, you can compile and run the Java code using the following commands:
```bash
# Compile the Java code
mvn clean package dependency:copy-dependencies -DincludeScope=runtime

# Run the CLI with an example JSON input
java -cp "target\classes;target\dependency\*" benchmark.Main help
```
