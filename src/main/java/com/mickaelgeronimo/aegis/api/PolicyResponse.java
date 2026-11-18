package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyEntity;

import java.time.Instant;

/**
 * External representation of a persisted rate-limit policy.
 */
public record PolicyResponse(
        String id,
        String tenantId,
        String routePattern,
        RateLimitAlgorithmType algorithm,
        long capacity,
        long refillTokens,
        long windowSeconds,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static PolicyResponse from(RateLimitPolicyEntity entity) {
        return new PolicyResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getRoutePattern(),
                entity.getAlgorithm(),
                entity.getCapacity(),
                entity.getRefillTokens(),
                entity.getWindowSeconds(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
