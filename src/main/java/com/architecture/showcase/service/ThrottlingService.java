package com.architecture.showcase.service;

import java.util.concurrent.Semaphore;

/**
 * Bounds how many transactions may be in flight against the upstream services at once.
 *
 * <p><strong>The bug this version fixes.</strong> The original released the permit in a
 * {@code finally} block that also ran when {@code acquire()} had thrown, so an interrupted
 * caller incremented the permit count without ever having taken one. Under repeated
 * interruption the semaphore silently grows past its limit and the throttle stops throttling -
 * a failure that only appears under cancellation pressure, which is exactly when the upstream
 * is already struggling.
 *
 * <p>Interruption is rethrown rather than swallowed. A caller that asked to be cancelled should
 * not have its task reported as completed.
 */
public class ThrottlingService {

    public static final int DEFAULT_MAX_CONCURRENT = 100;

    private final Semaphore permits;

    public ThrottlingService() {
        this(DEFAULT_MAX_CONCURRENT);
    }

    /**
     * @param maxConcurrent permits available at once; fair, so callers are served in arrival
     *                      order rather than by thread-scheduling luck
     */
    public ThrottlingService(int maxConcurrent) {
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException("maxConcurrent must be at least 1, got " + maxConcurrent);
        }
        this.permits = new Semaphore(maxConcurrent, true);
    }

    /**
     * Runs {@code task} while holding one permit, releasing it however the task ends.
     *
     * @throws IllegalStateException if the waiting thread is interrupted before a permit is
     *                               available; the interrupt flag is restored
     */
    public void executeThrottledTask(Runnable task) {
        boolean acquired = false;
        try {
            permits.acquire();
            acquired = true;
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a permit", e);
        } finally {
            // Only give back what was taken. Releasing unconditionally is the leak.
            if (acquired) {
                permits.release();
            }
        }
    }

    /** @return permits currently free; used by tests to prove none leaked. */
    public int availablePermits() {
        return permits.availablePermits();
    }
}
