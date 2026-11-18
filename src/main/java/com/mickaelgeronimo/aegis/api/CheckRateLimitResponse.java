package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.core.RateLimitDecision;

/**
 * Response summarizing the rate-limit decision for a check request.
 */
public record CheckRateLimitResponse(
        boolean allowed,
        long limit,
        long remaining,
        long resetEpochSeconds,
        long retryAfterSeconds,
        RateLimitAlgorithmType algorithm
) {
    public static CheckRateLimitResponse from(RateLimitDecision decision) {
        return new CheckRateLimitResponse(
                decision.allowed(),
                decision.limit(),
                decision.remaining(),
                decision.resetEpochSeconds(),
                decision.retryAfterSeconds(),
                decision.algorithm()
        );
    }
}
