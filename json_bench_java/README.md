# Java JSON benchmark CLI

A small Java JSON processor with jq-like commands and option to use single- or multi-threaded processing.

## Supported Commands:

- Engine:
  - `single`
  - `multi`
- Commands:
  - `<engine> length <file> [expr]`
  - `<engine> filter <file> [expr]`
  - `<engine> map <file> [expr]` (map_values style)
  - `help` / `h`

## Usage in CLI
From the project root, you can compile and run the Java code using the following commands:
```bash
# Compile the Java code
mvn clean package dependency:copy-dependencies -DincludeScope=runtime

# Show the built-in help
java -cp "target\classes;target\dependency\*" benchmark.Main help

# Run a command against a JSON file
java -cp "target\classes;target\dependency\*" benchmark.Main single length sample.json .filter
```
