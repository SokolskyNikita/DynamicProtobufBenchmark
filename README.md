# Protobuf Dynamic vs Normal Benchmark

This tiny repo demonstrates and benchmarks the performance difference between using normal (compiled) Protocol Buffers and Dynamic Protocol Buffers in Java.

## Overview

Protocol Buffers (Protobuf) is a method of serializing structured data. This benchmark compares two approaches:

1. Normal Protobuf: Using compiled Java classes generated from .proto files.
2. Dynamic Protobuf: Using DynamicMessage to dynamically load protobuf object instances.

The benchmark measures serialization and deserialization times for both approaches.

## Pre-requisites

As far as I can tell, you still have to precompile a `.desc` file in addition to having your `.proto` file. But pre-compiling the `.desc` file aside, dynamically loading arbitrary Protobufs in runtime is possible. The commands used were:

- To compile into a Java class: `protoc --java_out=. person.proto`
- To compile the `.desc` file: `protoc --include_imports --proto_path=. --descriptor_set_out=proto.desc person.proto`

## Results

Here are the results from a sample run with 1,000,000 iterations on a Macbook M1 Pro:

```
Normal Protobuf serialization time: 304 ms
Dynamic Protobuf serialization time: 1667 ms
Normal Protobuf deserialization time: 4596 ms
Dynamic Protobuf deserialization time: 5250 ms
```

These results show that:
- Normal Protobuf serialization is about 5.5 times faster than Dynamic Protobuf.
- Normal Protobuf deserialization is about 1.14 times faster than Dynamic Protobuf.
