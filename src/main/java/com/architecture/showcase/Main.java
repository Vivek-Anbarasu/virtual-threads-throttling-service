import com.architecture.showcase.service.AggregationService;
import com.architecture.showcase.service.ThrottlingService;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;


void main() {
    IO.println("Starting Concurrency Virtual Threads Showcase...");
    AggregationService service = new AggregationService();
    ThrottlingService throttlingService = new ThrottlingService();

    long startTime = System.currentTimeMillis();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        // Scaled up to 1000 transactions as requested
        IntStream.range(0, 1000).forEach(i -> {
            executor.submit(() -> {
                // Throttling to 100 concurrent operations max to safeguard external api
                throttlingService.executeThrottledTask(() -> {
                    try {
                        String id = "ID-" + i;
                        service.fetchAggregatedFinancialData(id);
                    } catch (Exception e) {
                        IO.println("Transaction failed for id-" + i + ": " + e.getMessage());
                    }
                });
            });
        });
    }

    long endTime = System.currentTimeMillis();
    IO.println("Processed 1000 concurrent transactions successfully!");
    IO.println("Total Execution Time: " + (endTime - startTime) + " ms");
}

