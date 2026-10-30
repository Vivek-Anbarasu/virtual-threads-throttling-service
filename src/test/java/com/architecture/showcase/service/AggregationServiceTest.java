package com.architecture.showcase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.architecture.showcase.domain.MarketData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AggregationServiceTest {

    @Test
    @DisplayName("combines both upstream calls into one view")
    void combinesBothCalls() throws Exception {
        var data = new AggregationService(new StubApi(101.5, "BULLISH"))
                .fetchAggregatedFinancialData("ID-7");

        assertEquals("ID-7", data.id());
        assertEquals(101.5, data.price(), 1e-9);
        assertEquals("BULLISH", data.sentiment());
    }

    /**
     * Proves concurrency without measuring time. Each stub call blocks until the other one has
     * also started; if the two calls ran sequentially the first would wait out its two-second
     * budget and fail the test. A timing assertion ("took less than 350 ms") would pass on a
     * slow machine and flake on a loaded CI runner - this one cannot.
     */
    @Test
    @DisplayName("runs the two calls at the same time, not one after the other")
    void runsBothCallsConcurrently() throws Exception {
        var bothInFlight = new CountDownLatch(2);

        MarketDataApi api = new MarketDataApi() {
            @Override
            public double fetchStockPrice() throws Exception {
                bothInFlight.countDown();
                if (!bothInFlight.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("price ran while sentiment was not in flight");
                }
                return 1.0;
            }

            @Override
            public String fetchAiSentiment() throws Exception {
                bothInFlight.countDown();
                if (!bothInFlight.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("sentiment ran while price was not in flight");
                }
                return "NEUTRAL";
            }
        };

        MarketData data = new AggregationService(api).fetchAggregatedFinancialData("ID-1");

        assertEquals(1.0, data.price(), 1e-9);
        assertEquals("NEUTRAL", data.sentiment());
    }

    @Test
    @DisplayName("a failing subtask fails the aggregation instead of returning half of it")
    void failureInOneSubtaskFailsTheAggregation() {
        var api = new StubApi(100.0, "BULLISH") {
            @Override
            public String fetchAiSentiment() {
                throw new IllegalStateException("sentiment upstream down");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> new AggregationService(api).fetchAggregatedFinancialData("ID-1"));
    }

    private static class StubApi implements MarketDataApi {

        private final double price;
        private final String sentiment;

        StubApi(double price, String sentiment) {
            this.price = price;
            this.sentiment = sentiment;
        }

        @Override
        public double fetchStockPrice() {
            return price;
        }

        @Override
        public String fetchAiSentiment() {
            return sentiment;
        }
    }
}
