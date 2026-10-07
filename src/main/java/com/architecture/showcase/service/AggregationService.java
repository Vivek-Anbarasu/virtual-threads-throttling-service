package com.architecture.showcase.service;

import com.architecture.showcase.domain.MarketData;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

public class AggregationService {

    private final MockExternalApi externalApi = new MockExternalApi();

    public MarketData fetchAggregatedFinancialData(String id) throws Exception {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<Double> priceTask = scope.fork(() -> externalApi.fetchStockPrice());
            Subtask<String> sentimentTask = scope.fork(() -> externalApi.fetchAiSentiment());
            scope.join();
            return new MarketData(id, priceTask.get(), sentimentTask.get());
        }
    }
}
