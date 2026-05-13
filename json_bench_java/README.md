# Java JSON benchmark CLI

A small Java JSON processor with jq-like commands and option to use single- or multi-threaded processing.

## Supported Commands:

- Engine:
  - `single`
  - `multi`

- Commands:
  - `length`
  - `filter` (path and value supported)
  - `map` (map_values style)
  - `help` / `h`

- Command Forms:
  - `<engine> length <file> [expr]`
  - `<engine> filter <file> [expr]`
  - `<engine> map <file> [expr]` (map_values style)
  - `help` / `h`

### Filter Expressions
1) path: `.foo.bar`
2) predicate: `== 42` or `!= "hello"`
   - `Syntax: .<arrayPath>[?<field><op><value>]`
   - Supported operators:
     - `==` (equals)
     - `!=` (not equals)
     - `>` (greater than)
     - `<` (less than)
   - Supported types:
     - numbers
     - booleans
     - strings
     - null

## Usage in CLI
From the project root, you can compile and run the Java code using the following commands:
```bash
# Compile the Java code
mvn clean package dependency:copy-dependencies -DincludeScope=runtime

# Show help
java -cp "target\classes;target\dependency\*" benchmark.Main help

# Run a command against a JSON file
java -cp "target\classes;target\dependency\*" benchmark.Main single length sample.json .foo

java -cp "target\classes;target\dependency\*" benchmark.Main single filter sample.json .foo[?id==0].bar
```
