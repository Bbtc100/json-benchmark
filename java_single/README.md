# java_single JSON benchmark CLI

A small single-threaded Java JSON processor with jq-like commands:

- `length`
- `filter`
- `map` (currently map_values style)
- `help` / `h`

Current suite covers CLI behavior (`Main`) and all command classes (`LengthCommand`, `FilterCommand`, `MapCommand`) including error and edge cases.

