package com.mickaelgeronimo.aegis.core;

import java.util.Objects;

/**
 * Low-overhead Fixed Window Counter limiter that resets usage at discrete window boundaries.
 */
public final class FixedWindowCounterRateLimiter implements RateLimiter {

    private final RateLimitConfig config;
    private final TimeSource timeSource;
    private long activeWindowIndex;
    private long usedPermits;

    public FixedWindowCounterRateLimiter(RateLimitConfig config, TimeSource timeSource) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
        this.activeWindowIndex = Math.floorDiv(timeSource.nanoTime(), config.windowNanos());
        this.usedPermits = 0L;
    }

    @Override
    public synchronized RateLimitDecision tryAcquire(long permits) {
        RateLimiter.validatePermits(permits);
        long nowNanos = timeSource.nanoTime();
        long nowEpoch = timeSource.epochSeconds();
        long windowNanos = config.windowNanos();
        long currentWindow = Math.floorDiv(nowNanos, windowNanos);

        if (currentWindow != activeWindowIndex) {
            activeWindowIndex = currentWindow;
            usedPermits = 0L;
        }

        long nanosUntilReset = windowNanos - Math.floorMod(nowNanos, windowNanos);

        if (permits <= config.capacity() && usedPermits + permits <= config.capacity()) {
            usedPermits += permits;
            long remaining = config.capacity() - usedPermits;
            return RateLimitDecision.allow(
                    config.capacity(),
                    remaining,
                    nanosUntilReset,
                    nowEpoch,
                    RateLimitAlgorithmType.FIXED_WINDOW_COUNTER
            );
        }

        long remaining = Math.max(0L, config.capacity() - usedPermits);
        return RateLimitDecision.reject(
                config.capacity(),
                remaining,
                nanosUntilReset,
                nanosUntilReset,
                nowEpoch,
                RateLimitAlgorithmType.FIXED_WINDOW_COUNTER
        );
    }

    @Override
    public RateLimitConfig config() {
        return config;
    }
}
