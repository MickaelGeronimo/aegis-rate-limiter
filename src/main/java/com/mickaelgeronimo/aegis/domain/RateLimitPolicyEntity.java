package com.mickaelgeronimo.aegis.domain;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.core.RateLimitConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent rate-limit quota policy scoped to a tenant and route pattern.
 */
@Entity
@Table(name = "rate_limit_policies")
public class RateLimitPolicyEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "route_pattern", nullable = false, length = 255)
    private String routePattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateLimitAlgorithmType algorithm;

    @Column(nullable = false)
    private long capacity;

    @Column(name = "refill_tokens", nullable = false)
    private long refillTokens;

    @Column(name = "window_seconds", nullable = false)
    private long windowSeconds;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RateLimitPolicyEntity() {
    }

    public RateLimitPolicyEntity(
            String tenantId,
            String routePattern,
            RateLimitAlgorithmType algorithm,
            long capacity,
            long refillTokens,
            long windowSeconds,
            boolean enabled
    ) {
        this.id = "pol_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.tenantId = tenantId;
        this.routePattern = routePattern;
        this.algorithm = algorithm;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.windowSeconds = windowSeconds;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public RateLimitConfig toConfig() {
        return new RateLimitConfig(algorithm, capacity, refillTokens, Duration.ofSeconds(windowSeconds));
    }

    public String configFingerprint() {
        return algorithm.name() + ":" + capacity + ":" + refillTokens + ":" + windowSeconds + ":" + enabled;
    }

    public void update(
            RateLimitAlgorithmType algorithm,
            long capacity,
            long refillTokens,
            long windowSeconds,
            boolean enabled
    ) {
        this.algorithm = algorithm;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.windowSeconds = windowSeconds;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRoutePattern() {
        return routePattern;
    }

    public RateLimitAlgorithmType getAlgorithm() {
        return algorithm;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
