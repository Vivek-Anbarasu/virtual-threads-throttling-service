# Java 27 Virtual Threads & Throttling

[![Java Version](https://img.shields.io/badge/Java-27-orange.svg)](https://jdk.java.net/27/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-blue.svg)](https://maven.apache.org/)

A high-performance Java project demonstrating concurrent financial data aggregation using **Virtual Threads** and **Structured Concurrency** natively on **JDK 27**. It highlights how to handle high-throughput, platform-independent executions while safely gating resource access via an explicit `ThrottlingService`.

## 🏗️ Architectural Overview

The project simulates a high-scale environment processing **1000 concurrent financial transactions**. For each transaction, a unique identifier (`ID-0` to `ID-999`) is assigned to isolate and trace operations.

```
                  [ 1000 Main Virtual Tasks ]
                               │
                ▼ (Throttled to 100 concurrent)
                    [ ThrottlingService ]
                      (Java Semaphore)
                               │
              ┌────────────────┴────────────────┐
              ▼                                 ▼
      [ Subtask: Price ]              [ Subtask: Sentiment ]
     MockExternalApi::fetch          MockExternalApi::fetch
```

### Key Design Patterns
1. **Lightweight Concurrency:** Uses `Executors.newVirtualThreadPerTaskExecutor()` to spin up millions of cheap, application-managed threads without saturating OS thread limits.
2. **Structured Concurrency:** Uses JDK 27's `StructuredTaskScope.open()` to model split/merge subtasks cleanly. If a sibling task fails, the entire scope fails fast and auto-closes, removing memory leaks.
3. **Concurrency Throttling:** Uses a `Semaphore` boundary inside `ThrottlingService` to limit the execution to **100 active parallel jobs**, preventing downstream target APIs from crashing due to high-volume connection requests.

---

## 🛠️ Project Structure

```text
src/main/java/com/architecture/
│
├── Main.java                        # System Entrypoint & Virtual Loop
└── showcase/
    ├── domain/
    │   └── MarketData.java          # Record representing aggregated results
    └── service/
        ├── AggregationService.java  # StructuredTaskScope Management
        ├── MockExternalApi.java     # Remote IO Latency Emulation
        └── ThrottlingService.java   # Semaphore-based Throttling Gating
```

---

## ⚙️ Prerequisites

To run or build this repository, ensure your environment fulfills the following baseline requisites:
* **Java Development Kit (JDK) 27** or higher.
* **Apache Maven 3.6.3** or higher.

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/Vivek-Anbarasu/virtual-threads-throttling-service.git
cd virtual-threads-throttling-service
```

### 2. Compile the Project
The project uses custom JVM preview flags defined in the `maven-compiler-plugin` configuration block. Compile the sources using:
```bash
mvn clean compile
```

### 3. Run the Showcase Application
Execute the compiled project through the `exec-maven-plugin` wrapper, which injects runtime `--enable-preview` parameters natively:
```bash
mvn exec:java
```

---

## 📊 Sample Execution Log

Upon launching, the console output trace should look like the following:

```text
Starting Concurrency Virtual Threads Showcase...
Processed 1000 concurrent transactions successfully!
Total Execution Time: 2540 ms
```
*(Execution time will vary based on hardware thread allocation capacity and internal service thread scheduling efficiency).*
