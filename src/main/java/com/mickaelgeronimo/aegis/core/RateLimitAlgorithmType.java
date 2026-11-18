package com.mickaelgeronimo.aegis.core;

/**
 * Supported rate limiting algorithms implemented by Aegis.
 */
public enum RateLimitAlgorithmType {
    TOKEN_BUCKET,
    SLIDING_WINDOW_LOG,
    FIXED_WINDOW_COUNTER,
    LEAKY_BUCKET
}
