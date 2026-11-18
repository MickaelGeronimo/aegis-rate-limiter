package com.mickaelgeronimo.aegis.core;

/**
 * Outcome of evaluating a single rate-limit request against a limiter bucket.
 */
public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        long resetEpochSeconds,
        long retryAfterSeconds,
        RateLimitAlgorithmType algorithm
) {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    public static RateLimitDecision allow(
            long limit,
            long remaining,
            long resetInNanos,
            long nowEpochSeconds,
            RateLimitAlgorithmType algorithm
    ) {
        long resetAt = nowEpochSeconds + ceilSeconds(Math.max(0L, resetInNanos));
        return new RateLimitDecision(true, limit, Math.max(0L, remaining), resetAt, 0L, algorithm);
    }

    public static RateLimitDecision reject(
            long limit,
            long remaining,
            long retryInNanos,
            long resetInNanos,
            long nowEpochSeconds,
            RateLimitAlgorithmType algorithm
    ) {
        long retrySeconds = Math.max(1L, ceilSeconds(Math.max(1L, retryInNanos)));
        long resetAt = nowEpochSeconds + ceilSeconds(Math.max(retryInNanos, resetInNanos));
        return new RateLimitDecision(false, limit, Math.max(0L, remaining), resetAt, retrySeconds, algorithm);
    }

    public static long ceilSeconds(long nanos) {
        if (nanos <= 0L) {
            return 0L;
        }
        return (nanos + NANOS_PER_SECOND - 1L) / NANOS_PER_SECOND;
    }
}
