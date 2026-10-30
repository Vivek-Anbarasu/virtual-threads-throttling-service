import com.architecture.showcase.service.AggregationService;
import com.architecture.showcase.service.ThrottlingService;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Demo entry point: 1,000 transactions, each fanning out to two upstream calls, with at most
 * {@value ThrottlingService#DEFAULT_MAX_CONCURRENT} transactions in flight at once.
 *
 * <p>Run with {@code mvn compile exec:java} (the exec plugin passes {@code --enable-preview}).
 *
 * <p>The summary reports what actually happened. The previous version printed
 * "Processed 1000 concurrent transactions successfully!" unconditionally, including when every
 * task had failed - which made the demo useless as evidence of anything.
 */
void main() {
    int transactions = 1_000;
    int maxConcurrent = ThrottlingService.DEFAULT_MAX_CONCURRENT;

    var aggregation = new AggregationService();
    var throttling = new ThrottlingService(maxConcurrent);

    var succeeded = new AtomicInteger();
    var failed = new AtomicInteger();

    long startedAt = System.nanoTime();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        IntStream.range(0, transactions).forEach(i ->
                executor.submit(() -> throttling.executeThrottledTask(() -> {
                    try {
                        aggregation.fetchAggregatedFinancialData("ID-" + i);
                        succeeded.incrementAndGet();
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                })));
    }

    long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

    IO.println("Aggregated %d of %d transactions in %d ms (throttle: %d concurrent, %d failed)"
            .formatted(succeeded.get(), transactions, elapsedMillis, maxConcurrent, failed.get()));
}
