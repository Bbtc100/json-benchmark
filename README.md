# JSON processing software benchmark

This project focuses on benchmarking JSON processing performance by comparing custom implementations with **jq**.

## Goals

The goal is to implement multiple basic JSON processing solutions and analyze their performance on different file sizes. The comparison emphasizes execution time, CPU usage and memory consumption.

## Implementations

### Java

- Single-threaded implementation
- Multi-threaded implementation

### Python

- Single-threaded implementation

### [jq](https://jqlang.org/)
- Popular, lightweight command-line JSON processor

## Supported Operations

The following JSON processing tasks are implemented (for now):

- **Filtering**
- **Length calculation**
- **Mapping**

## Benchmarking Methodology

All benchmarks are evaluated using `Hyperfine`.

Benchmarks are executed on **synthetically generated JSON datasets** of varying sizes.

## Objective

The project aims to provide insights into:

- Performance differences between single-threaded and multi-threaded processing
- Impact of implementation language
- How custom solutions compare to `jq` **under controlled conditions**
