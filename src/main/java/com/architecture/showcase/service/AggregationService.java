package com.architecture.showcase.service;

import com.architecture.showcase.domain.MarketData;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * Fans two upstream calls out and joins them under one scope.
 *
 * <p><strong>Why a scope rather than two futures.</strong> The two calls are only useful
 * together: a price without a sentiment is not an aggregation. A scope makes that structural
 * rather than conventional - if either subtask fails, {@code join()} fails the whole
 * aggregation and the sibling is cancelled, so there is no path where a half-built
 * {@link MarketData} escapes because the caller forgot to check one of two futures.
 *
 * <p>The scope is used through try-with-resources because closing it is what guarantees every
 * forked subtask has finished before the method returns, including on the exception path.
 *
 * <p><strong>Where throttling belongs.</strong> Not here. This method is the unit of work a
 * caller wants to bound, and a permit held per <em>upstream call</em> would throttle the
 * fan-out instead of the transactions - 100 permits would become 50 aggregations. See
 * {@link ThrottlingService}.
 */
public class AggregationService {

    private final MarketDataApi externalApi;

    /** Uses the demo client; tests inject a stub through the other constructor. */
    public AggregationService() {
        this(new MockExternalApi());
    }

    public AggregationService(MarketDataApi externalApi) {
        this.externalApi = externalApi;
    }

    /**
     * @throws Exception the failure of the first subtask that failed, after the sibling has
     *                   been cancelled and the scope closed. {@code ExecutionException} is
     *                   unwrapped rather than propagated: it is an artefact of how the scope
     *                   joins, and a caller should not have to know that the two calls were
     *                   concurrent in order to catch a domain failure.
     */
    public MarketData fetchAggregatedFinancialData(String id) throws Exception {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<Double> priceTask = scope.fork(externalApi::fetchStockPrice);
            Subtask<String> sentimentTask = scope.fork(externalApi::fetchAiSentiment);

            scope.join();

            return new MarketData(id, priceTask.get(), sentimentTask.get());
        } catch (ExecutionException e) {
            throw unwrap(e);
        }
    }

    private static Exception unwrap(ExecutionException e) {
        if (e.getCause() instanceof Exception cause) {
            return cause;
        }
        if (e.getCause() instanceof Error cause) {
            throw cause;
        }
        return e;
    }
}
