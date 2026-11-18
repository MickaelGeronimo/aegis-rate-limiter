package com.mickaelgeronimo.aegis.core;

/**
 * Contract for thread-safe rate limiter implementations.
 */
public interface RateLimiter {

    RateLimitDecision tryAcquire(long permits);

    default RateLimitDecision tryAcquire() {
        return tryAcquire(1L);
    }

    RateLimitConfig config();

    static void validatePermits(long permits) {
        if (permits <= 0L) {
            throw new IllegalArgumentException("permits must be greater than 0");
        }
    }
}
