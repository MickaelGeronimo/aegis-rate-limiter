package com.mickaelgeronimo.aegis.core;

import java.util.Objects;

/**
 * Leaky Bucket limiter enforcing smooth outflow rates by draining queued water level at a constant rate.
 */
public final class LeakyBucketRateLimiter implements RateLimiter {

    private final RateLimitConfig config;
    private final TimeSource timeSource;
    private final double leakRatePerNano;
    private double waterLevel;
    private long lastLeakNanos;

    public LeakyBucketRateLimiter(RateLimitConfig config, TimeSource timeSource) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
        this.leakRatePerNano = (double) config.refillTokens() / config.windowNanos();
        this.waterLevel = 0.0;
        this.lastLeakNanos = timeSource.nanoTime();
    }

    @Override
    public synchronized RateLimitDecision tryAcquire(long permits) {
        RateLimiter.validatePermits(permits);
        long nowNanos = timeSource.nanoTime();
        long nowEpoch = timeSource.epochSeconds();
        leak(nowNanos);

        if (permits > config.capacity()) {
            long waitNanos = config.windowNanos();
            long remaining = Math.max(0L, (long) Math.floor(config.capacity() - waterLevel));
            return RateLimitDecision.reject(
                    config.capacity(),
                    remaining,
                    waitNanos,
                    waitNanos,
                    nowEpoch,
                    RateLimitAlgorithmType.LEAKY_BUCKET
            );
        }

        if (waterLevel + permits <= config.capacity()) {
            waterLevel += permits;
            long remaining = Math.max(0L, (long) Math.floor(config.capacity() - waterLevel));
            long drainNanos = (long) Math.ceil(waterLevel / leakRatePerNano);
            return RateLimitDecision.allow(
                    config.capacity(),
                    remaining,
                    drainNanos,
                    nowEpoch,
                    RateLimitAlgorithmType.LEAKY_BUCKET
            );
        }

        double overflow = (waterLevel + permits) - config.capacity();
        long waitNanos = (long) Math.ceil(overflow / leakRatePerNano);
        long drainNanos = (long) Math.ceil(waterLevel / leakRatePerNano);
        long remaining = Math.max(0L, (long) Math.floor(config.capacity() - waterLevel));

        return RateLimitDecision.reject(
                config.capacity(),
                remaining,
                waitNanos,
                drainNanos,
                nowEpoch,
                RateLimitAlgorithmType.LEAKY_BUCKET
        );
    }

    private void leak(long nowNanos) {
        long elapsed = nowNanos - lastLeakNanos;
        if (elapsed > 0L) {
            waterLevel = Math.max(0.0, waterLevel - elapsed * leakRatePerNano);
            lastLeakNanos = nowNanos;
        }
    }

    @Override
    public RateLimitConfig config() {
        return config;
    }
}
