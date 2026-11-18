package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload to register a new tenant rate-limit policy.
 */
public record CreatePolicyRequest(
        @NotBlank String tenantId,
        @NotBlank String routePattern,
        @NotNull RateLimitAlgorithmType algorithm,
        @Positive long capacity,
        @Positive long refillTokens,
        @Positive long windowSeconds,
        Boolean enabled
) {
    public boolean isEnabledOrDefault() {
        return enabled == null || enabled;
    }
}
