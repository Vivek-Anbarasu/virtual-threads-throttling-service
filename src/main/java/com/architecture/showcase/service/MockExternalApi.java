package com.architecture.showcase.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for two external services with realistic, unequal latencies.
 *
 * <p>Interruption is propagated rather than logged and ignored. The previous version caught
 * {@link InterruptedException}, re-set the flag, and then returned a fabricated value - so a
 * cancelled aggregation still produced a price, and the cancellation was invisible to the
 * caller.
 */
public class MockExternalApi implements MarketDataApi {

    private static final Duration PRICE_LATENCY = Duration.ofMillis(100);
    private static final Duration SENTIMENT_LATENCY = Duration.ofMillis(250);

    @Override
    public double fetchStockPrice() throws InterruptedException {
        Thread.sleep(PRICE_LATENCY);
        return 100.25 + (ThreadLocalRandom.current().nextDouble() * 10);
    }

    @Override
    public String fetchAiSentiment() throws InterruptedException {
        Thread.sleep(SENTIMENT_LATENCY);
        return ThreadLocalRandom.current().nextBoolean() ? "BULLISH" : "BEARISH";
    }
}
