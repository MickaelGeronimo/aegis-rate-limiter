package com.mickaelgeronimo.aegis.core;

import java.util.Objects;

/**
 * Thread-safe Token Bucket limiter supporting controlled bursts and continuous nanosecond-precision refill.
 */
public final class TokenBucketRateLimiter implements RateLimiter {

    private final RateLimitConfig config;
    private final TimeSource timeSource;
    private final double refillRatePerNano;
    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(RateLimitConfig config, TimeSource timeSource) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource must not be null");
        this.refillRatePerNano = (double) config.refillTokens() / config.windowNanos();
        this.availableTokens = config.capacity();
        this.lastRefillNanos = timeSource.nanoTime();
    }

    @Override
    public synchronized RateLimitDecision tryAcquire(long permits) {
        RateLimiter.validatePermits(permits);
        long nowNanos = timeSource.nanoTime();
        long nowEpoch = timeSource.epochSeconds();
        refill(nowNanos);

        if (permits > config.capacity()) {
            long waitNanos = config.windowNanos();
            return RateLimitDecision.reject(
                    config.capacity(),
                    (long) Math.floor(availableTokens),
                    waitNanos,
                    waitNanos,
                    nowEpoch,
                    RateLimitAlgorithmType.TOKEN_BUCKET
            );
        }

        if (availableTokens >= permits) {
            availableTokens -= permits;
            long remaining = (long) Math.floor(availableTokens);
            long resetNanos = (long) Math.ceil((config.capacity() - availableTokens) / refillRatePerNano);
            return RateLimitDecision.allow(
                    config.capacity(),
                    remaining,
                    resetNanos,
                    nowEpoch,
                    RateLimitAlgorithmType.TOKEN_BUCKET
            );
        }

        double deficit = permits - availableTokens;
        long waitNanos = (long) Math.ceil(deficit / refillRatePerNano);
        long resetNanos = (long) Math.ceil((config.capacity() - availableTokens) / refillRatePerNano);
        return RateLimitDecision.reject(
                config.capacity(),
                (long) Math.floor(availableTokens),
                waitNanos,
                resetNanos,
                nowEpoch,
                RateLimitAlgorithmType.TOKEN_BUCKET
        );
    }

    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed > 0L) {
            availableTokens = Math.min(config.capacity(), availableTokens + elapsed * refillRatePerNano);
            lastRefillNanos = nowNanos;
        }
    }

    @Override
    public RateLimitConfig config() {
        return config;
    }
}
