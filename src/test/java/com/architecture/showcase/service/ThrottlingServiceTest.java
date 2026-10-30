package com.architecture.showcase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThrottlingServiceTest {

    @Test
    @DisplayName("never admits more tasks than it has permits")
    void neverExceedsThePermitLimit() throws Exception {
        int limit = 4;
        int tasks = 40;

        var service = new ThrottlingService(limit);
        var inFlight = new AtomicInteger();
        var peak = new AtomicInteger();
        var finished = new CountDownLatch(tasks);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> service.executeThrottledTask(() -> {
                    peak.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        inFlight.decrementAndGet();
                        finished.countDown();
                    }
                }));
            }
            assertTrue(finished.await(10, TimeUnit.SECONDS), "throttled tasks did not finish");
        }

        assertTrue(peak.get() > 0, "no task ever ran, so the assertion below would be vacuous");
        assertTrue(peak.get() <= limit,
                "peak concurrency was " + peak.get() + ", above the permit limit of " + limit);
        assertEquals(limit, service.availablePermits(), "permits were leaked");
    }

    @Test
    @DisplayName("returns its permit when the task throws")
    void releasesThePermitWhenTheTaskFails() {
        var service = new ThrottlingService(2);

        assertThrows(IllegalStateException.class, () -> service.executeThrottledTask(() -> {
            throw new IllegalStateException("upstream exploded");
        }));

        assertEquals(2, service.availablePermits(), "a failed task leaked its permit");
    }

    @Test
    @DisplayName("rejects a permit count that would block forever")
    void rejectsAZeroPermitLimit() {
        assertThrows(IllegalArgumentException.class, () -> new ThrottlingService(0));
    }
}
