package com.gesmio.relay.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(Long endpointId, int ratePerSecond) {
        Bucket bucket = buckets.computeIfAbsent(endpointId, id -> newBucket(ratePerSecond));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(int ratePerSecond) {
        Bandwidth limit = Bandwidth.classic(ratePerSecond, Refill.greedy(ratePerSecond, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
