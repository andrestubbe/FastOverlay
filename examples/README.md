# FastOverlay Examples

This folder contains standalone example projects to demonstrate and test the library.

## Demo

The `Demo` project provides a simple "Hello World" implementation.

To run it locally using the JAR you just built:
```bash
cd Demo
mvn compile exec:java
```

## Benchmark

The `Benchmark` project provides JMH (Java Microbenchmark Harness) performance measurements for native DirectComposition overlay operations.

To run it:
```bash
# From module root:
run-benchmark.bat

# Or manually:
cd examples/Benchmark
mvn clean package -DskipTests
java --enable-native-access=ALL-UNNAMED -jar target/benchmarks.jar
```
