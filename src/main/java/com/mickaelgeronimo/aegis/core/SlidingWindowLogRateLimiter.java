package com.mickaelgeronimo.aegis.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Exact Sliding Window Log rate limiter tracking individual permit timestamps to eliminate boundary bursts.
 */
public final class SlidingWindowLogRateLimiter implements RateLimiter {

    private final RateLimitConfig config;
    private final TimeSource timeSource;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public SlidingWindowLogRateLimiter(RateLimitConfig config, TimeSource timeSource) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
    }

    @Override
    public synchronized RateLimitDecision tryAcquire(long permits) {
        RateLimiter.validatePermits(permits);
        long nowNanos = timeSource.nanoTime();
        long nowEpoch = timeSource.epochSeconds();
        long windowNanos = config.windowNanos();

        evictExpired(nowNanos, windowNanos);

        if (permits > config.capacity()) {
            long remaining = Math.max(0L, config.capacity() - timestamps.size());
            return RateLimitDecision.reject(
                    config.capacity(),
                    remaining,
                    windowNanos,
                    windowNanos,
                    nowEpoch,
                    RateLimitAlgorithmType.SLIDING_WINDOW_LOG
            );
        }

        if (timestamps.size() + permits <= config.capacity()) {
            for (long i = 0; i < permits; i++) {
                timestamps.addLast(nowNanos);
            }
            long remaining = config.capacity() - timestamps.size();
            long resetNanos = computeFullResetNanos(nowNanos, windowNanos);
            return RateLimitDecision.allow(
                    config.capacity(),
                    remaining,
                    resetNanos,
                    nowEpoch,
                    RateLimitAlgorithmType.SLIDING_WINDOW_LOG
            );
        }

        long overflowCount = (timestamps.size() + permits) - config.capacity();
        long targetTimestamp = findNthOldest(overflowCount);
        long retryNanos = Math.max(1L, (targetTimestamp + windowNanos) - nowNanos);
        long resetNanos = computeFullResetNanos(nowNanos, windowNanos);
        long remaining = Math.max(0L, config.capacity() - timestamps.size());

        return RateLimitDecision.reject(
                config.capacity(),
                remaining,
                retryNanos,
                resetNanos,
                nowEpoch,
                RateLimitAlgorithmType.SLIDING_WINDOW_LOG
        );
    }

    private void evictExpired(long nowNanos, long windowNanos) {
        while (!timestamps.isEmpty() && (nowNanos - timestamps.peekFirst()) >= windowNanos) {
            timestamps.removeFirst();
        }
    }

    private long findNthOldest(long n) {
        long index = 0L;
        for (Long ts : timestamps) {
            index++;
            if (index >= n) {
                return ts;
            }
        }
        return timestamps.peekFirst() != null ? timestamps.peekFirst() : 0L;
    }

    private long computeFullResetNanos(long nowNanos, long windowNanos) {
        Long oldest = timestamps.peekFirst();
        if (oldest == null) {
            return 0L;
        }
        return Math.max(0L, (oldest + windowNanos) - nowNanos);
    }

    @Override
    public RateLimitConfig config() {
        return config;
    }
}
