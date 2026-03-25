# JSON processing software benchmark

This project focuses on benchmarking JSON processing performance by comparing custom implementations with **jq**.

## Goals

The goal is to implement multiple basic JSON processing solutions and analyze their performance on different file sizes. The comparison emphasizes execution time, CPU usage and memory consumption.

## Implementations

### Java

- Single-threaded implementation
- Multi-threaded implementation
- JSON parsing using the **Jackson** library

### Python

- Single-threaded implementation
- JSON parsing using the built-in `json` module

## Supported Operations

The following JSON processing tasks will be implemented (for now):

- **Filtering**
- **Length calculation**
- **Mapping**

## Benchmarking Methodology

Performance will be evaluated using:

- **Java:** JMH (Java Microbenchmark Harness)
- **Python:** pyperf

Benchmarks will be executed on **synthetically generated JSON datasets** of varying sizes.

## Metrics

The following metrics will be collected:

- Execution time
- CPU usage
- Memory usage

## Input Data

JSON files of different sizes will be generated to test scalability and performance under increasing load.

## Objective

The project aims to provide insights into:

- Performance differences between single-threaded and multi-threaded processing
- Impact of implementation language
- How custom solutions compare to *jq* under controlled conditions
