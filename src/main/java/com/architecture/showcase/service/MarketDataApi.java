package com.architecture.showcase.service;

/**
 * The two independent upstream calls that make up one aggregated market view.
 *
 * <p>An interface rather than a concrete client so the aggregation can be tested without
 * sleeping: the demo implementation deliberately burns 100 ms and 250 ms, which is the
 * whole point of running them concurrently, but is useless as a test fixture.
 *
 * <p>Both methods declare {@code throws Exception} so an implementation is free to
 * propagate interruption rather than swallow it. A swallowed {@link InterruptedException}
 * leaves the thread's interrupt flag set but the caller believing the call succeeded.
 */
public interface MarketDataApi {

    double fetchStockPrice() throws Exception;

    String fetchAiSentiment() throws Exception;
}
