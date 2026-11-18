package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.domain.RateLimitAuditLogEntity;

import java.time.Instant;

/**
 * External representation of a blocked rate-limit audit event.
 */
public record AuditLogResponse(
        String id,
        String policyId,
        String tenantId,
        String routePath,
        String clientKey,
        RateLimitAlgorithmType algorithm,
        long permitsRequested,
        boolean allowed,
        long remaining,
        long retryAfterSeconds,
        Instant occurredAt
) {
    public static AuditLogResponse from(RateLimitAuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getPolicyId(),
                entity.getTenantId(),
                entity.getRoutePath(),
                entity.getClientKey(),
                entity.getAlgorithm(),
                entity.getPermitsRequested(),
                entity.isAllowed(),
                entity.getRemaining(),
                entity.getRetryAfterSeconds(),
                entity.getOccurredAt()
        );
    }
}
