package com.architecture.showcase.service;

import java.time.Duration;

public class MockExternalApi {

    public double fetchStockPrice() {
        try {
            Thread.sleep(Duration.ofMillis(100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 100.25 + (Math.random() * 10);
    }

    public String fetchAiSentiment() {
        try {
            Thread.sleep(Duration.ofMillis(250)); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Math.random() > 0.5 ? "BULLISH" : "BEARISH";
    }
}
