package com.architecture.showcase.service;

import java.util.concurrent.Semaphore;

public class ThrottlingService {
    private final Semaphore semaphore = new Semaphore(100);

    public void executeThrottledTask(Runnable task) {
        try {
            semaphore.acquire();
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();
        }
    }
}
