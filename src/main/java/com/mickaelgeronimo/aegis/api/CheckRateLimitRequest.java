package com.mickaelgeronimo.aegis.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request asking Aegis whether a client may consume permits under a tenant route policy.
 */
public record CheckRateLimitRequest(
        @NotBlank String tenantId,
        @NotBlank String routePath,
        @NotBlank String clientKey,
        @Positive Long permits
) {
    public long permitsOrDefault() {
        return permits == null ? 1L : permits;
    }
}
