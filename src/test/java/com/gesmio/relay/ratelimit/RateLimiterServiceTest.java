package com.gesmio.relay.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiter = new RateLimiterService();

    @Test
    void allowsUpToConfiguredCapacityThenBlocks() {
        long endpointId = 1L;
        int ratePerSecond = 2;

        assertThat(rateLimiter.tryConsume(endpointId, ratePerSecond)).isTrue();
        assertThat(rateLimiter.tryConsume(endpointId, ratePerSecond)).isTrue();
        assertThat(rateLimiter.tryConsume(endpointId, ratePerSecond)).isFalse();
    }

    @Test
    void tracksEachEndpointIndependently() {
        int ratePerSecond = 1;

        assertThat(rateLimiter.tryConsume(10L, ratePerSecond)).isTrue();
        assertThat(rateLimiter.tryConsume(10L, ratePerSecond)).isFalse();

        assertThat(rateLimiter.tryConsume(20L, ratePerSecond)).isTrue();
    }
}
