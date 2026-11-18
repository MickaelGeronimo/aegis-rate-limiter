package com.mickaelgeronimo.aegis.core;

/**
 * Factory creating the concrete {@link RateLimiter} implementation for a given configuration.
 */
public final class RateLimiterFactory {

    private RateLimiterFactory() {
    }

    public static RateLimiter create(RateLimitConfig config, TimeSource timeSource) {
        return switch (config.algorithm()) {
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(config, timeSource);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(config, timeSource);
            case FIXED_WINDOW_COUNTER -> new FixedWindowCounterRateLimiter(config, timeSource);
            case LEAKY_BUCKET -> new LeakyBucketRateLimiter(config, timeSource);
        };
    }

    public static RateLimiter create(RateLimitConfig config) {
        return create(config, TimeSource.SYSTEM);
    }
}
