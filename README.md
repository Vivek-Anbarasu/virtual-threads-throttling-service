# Virtual Threads & Structured Concurrency, with an Explicit Throttle

[![CI](https://github.com/Vivek-Anbarasu/virtual-threads-throttling-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Vivek-Anbarasu/virtual-threads-throttling-service/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-27%20preview-orange.svg)](https://openjdk.org/jeps/505)

A small, tested demonstration of the two Java concurrency features that actually change how a
service is written — **virtual threads** and **structured concurrency** — plus the part most
demos skip: what happens when the thing you are fanning out to has a rate limit.

One transaction fans out to two upstream calls with unequal latency. A thousand transactions
run at once. At most a hundred are ever in flight.

## The problem this models

| Call | Latency | |
| --- | --- | --- |
| `fetchStockPrice()` | 100 ms | independent |
| `fetchAiSentiment()` | 250 ms | independent |

Sequential execution costs 350 ms per transaction. The calls are independent, so the only
correct answer is 250 ms — and the only way to get it without leaking threads is to run them
concurrently and join them as a unit.

## The design

```
  1,000 virtual threads (one per transaction)
                │
                ▼
      ThrottlingService ── Semaphore(100, fair)
                │           bounds transactions, not calls
                ▼
      AggregationService ── StructuredTaskScope.open()
           │                    │
           ▼                    ▼
   fetchStockPrice()    fetchAiSentiment()
        100 ms               250 ms
```

Three decisions worth stating:

**The scope, not two futures.** A price without a sentiment is not an aggregation. A
`StructuredTaskScope` makes that structural: if either subtask fails, `join()` fails the whole
aggregation and cancels the sibling. With two `Future`s there is a path where the caller checks
one, forgets the other, and returns half a result.

**Throttle the transaction, not the call.** The permit is taken around the whole aggregation,
not around each upstream call. Throttling per call would halve the effective limit — 100 permits
would admit 50 transactions, each holding two.

**Release only what you took.** The permit is released in a `finally` guarded by an `acquired`
flag. Releasing unconditionally is a real bug: an interrupted caller that never acquired a
permit still increments the count, so under cancellation pressure the semaphore drifts above its
limit and the throttle silently stops throttling.

## Measured

```
$ java --enable-preview -cp target/classes Main
Aggregated 1000 of 1000 transactions in 2555 ms (throttle: 100 concurrent, 0 failed)
```

That is 1,000 transactions ÷ 100 permits = 10 waves × ~250 ms. The arithmetic and the
measurement agree, which is the only reason the number means anything.

## The preview constraint

**`StructuredTaskScope` is still a preview API in Java 27.** This is not a detail to gloss over:

- Compiled classes carry the preview minor version and **will not load on JDK 25 or 26** —
  they fail with `UnsupportedClassVersionError`, not a compile error.
- `--enable-preview` is required at compile time *and* at runtime. The POM passes it to both
  `maven-compiler-plugin` and the surefire JVM; the command below passes it to `java`.
- The build is pinned to `<maven.compiler.release>27</maven.compiler.release>` and CI pins
  JDK 27, deliberately, rather than `latest`.

When the API finalises, the `--enable-preview` flags come out and the pin can relax.

## Build and run

```bash
mvn -q compile
java --enable-preview -cp target/classes Main
```

`mvn exec:java` will **not** work: it runs in Maven's own JVM, which is not started with
`--enable-preview`, so it cannot load the classes.

## Tests

```bash
mvn test     # 6 tests
```

The suite asserts properties rather than timings, because a "took less than 350 ms" assertion
passes on a fast laptop and flakes on a loaded CI runner:

- **Concurrency is proven with a latch, not a stopwatch.** Each stub call counts down and then
  waits for the other to also be in flight. If the two calls ran sequentially, the first would
  wait out its two-second budget and fail. There is no clock in the assertion.
- **The permit limit is never exceeded** — 40 tasks against 4 permits, with peak concurrency
  sampled and asserted, and the permit count checked afterwards.
- **A failing task returns its permit**, and a zero-permit throttle is rejected rather than
  deadlocking every caller.
- **A failing subtask fails the aggregation** instead of yielding a partially built result.

## Layout

```
src/main/java/com/architecture/showcase/
├── Main.java                          demo entry point (implicit class, Java 25+)
├── domain/MarketData.java             the aggregate: id, price, sentiment
└── service/
    ├── MarketDataApi.java             the two upstream calls, injectable for tests
    ├── MockExternalApi.java           demo client: 100 ms and 250 ms of latency
    ├── AggregationService.java        structured fan-out and join
    └── ThrottlingService.java         fair semaphore, leak-free release
```

## Licence

MIT — see [LICENSE](LICENSE).
