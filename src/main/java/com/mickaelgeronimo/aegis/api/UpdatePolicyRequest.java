package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload to modify an existing rate-limit policy.
 */
public record UpdatePolicyRequest(
        @NotNull RateLimitAlgorithmType algorithm,
        @Positive long capacity,
        @Positive long refillTokens,
        @Positive long windowSeconds,
        boolean enabled
) {
}
