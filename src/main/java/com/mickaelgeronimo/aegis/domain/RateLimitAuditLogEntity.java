package com.mickaelgeronimo.aegis.domain;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit entry recording rate-limit enforcement outcomes for observability and compliance.
 */
@Entity
@Table(name = "rate_limit_audit_logs")
public class RateLimitAuditLogEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "policy_id", length = 64)
    private String policyId;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "route_path", nullable = false, length = 255)
    private String routePath;

    @Column(name = "client_key", nullable = false, length = 160)
    private String clientKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateLimitAlgorithmType algorithm;

    @Column(name = "permits_requested", nullable = false)
    private long permitsRequested;

    @Column(nullable = false)
    private boolean allowed;

    @Column(nullable = false)
    private long remaining;

    @Column(name = "retry_after_seconds", nullable = false)
    private long retryAfterSeconds;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected RateLimitAuditLogEntity() {
    }

    public RateLimitAuditLogEntity(
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
        this.id = "aud_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.policyId = policyId;
        this.tenantId = tenantId;
        this.routePath = routePath;
        this.clientKey = clientKey;
        this.algorithm = algorithm;
        this.permitsRequested = permitsRequested;
        this.allowed = allowed;
        this.remaining = remaining;
        this.retryAfterSeconds = retryAfterSeconds;
        this.occurredAt = occurredAt;
    }

    public String getId() {
        return id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRoutePath() {
        return routePath;
    }

    public String getClientKey() {
        return clientKey;
    }

    public RateLimitAlgorithmType getAlgorithm() {
        return algorithm;
    }

    public long getPermitsRequested() {
        return permitsRequested;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
