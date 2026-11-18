package com.mickaelgeronimo.aegis.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration defining capacity, refill/leak quota and window duration.
 */
public record RateLimitConfig(
        RateLimitAlgorithmType algorithm,
        long capacity,
        long refillTokens,
        Duration window
) {
    public RateLimitConfig {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than 0");
        }
        if (refillTokens <= 0) {
            throw new IllegalArgumentException("refillTokens must be greater than 0");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
    }

    public long windowNanos() {
        return window.toNanos();
    }
}
