package com.gesmio.relay.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fixed-window rate limiter backed by Redis, so the limit is enforced consistently
 * across every instance of the app rather than per-process.
 */
@Component
public class RateLimiterService {

    private static final String WINDOW_MILLIS = "1000";

    // Atomically increments the per-endpoint counter for the current 1s window and, on the
    // first hit of a new window, sets it to expire — avoiding a race between INCR and EXPIRE.
    private static final String SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                return 0
            else
                return 1
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>(SCRIPT, Long.class);
    }

    public boolean tryConsume(Long endpointId, int ratePerSecond) {
        String key = "relay:ratelimit:endpoint:" + endpointId;
        Long allowed = redisTemplate.execute(script, List.of(key), String.valueOf(ratePerSecond), WINDOW_MILLIS);
        return allowed != null && allowed == 1L;
    }
}
