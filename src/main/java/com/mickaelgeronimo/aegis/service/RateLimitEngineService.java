package com.mickaelgeronimo.aegis.service;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.core.RateLimitConfig;
import com.mickaelgeronimo.aegis.core.RateLimitDecision;
import com.mickaelgeronimo.aegis.core.RateLimiter;
import com.mickaelgeronimo.aegis.core.RateLimiterFactory;
import com.mickaelgeronimo.aegis.core.TimeSource;
import com.mickaelgeronimo.aegis.domain.RateLimitAuditLogEntity;
import com.mickaelgeronimo.aegis.domain.RateLimitAuditLogRepository;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyEntity;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime enforcement engine resolving tenant policies, managing active limiter buckets,
 * recording Micrometer counters and persisting audit logs for blocked requests.
 */
@Service
public class RateLimitEngineService {

    private static final String WILDCARD_ROUTE = "/**";
    private static final RateLimitConfig FALLBACK_CONFIG = new RateLimitConfig(
            RateLimitAlgorithmType.TOKEN_BUCKET, 60L, 60L, Duration.ofSeconds(60));

    private final RateLimitPolicyRepository policyRepository;
    private final RateLimitAuditLogRepository auditLogRepository;
    private final TimeSource timeSource;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    public RateLimitEngineService(
            RateLimitPolicyRepository policyRepository,
            RateLimitAuditLogRepository auditLogRepository,
            TimeSource timeSource,
            MeterRegistry meterRegistry
    ) {
        this.policyRepository = policyRepository;
        this.auditLogRepository = auditLogRepository;
        this.timeSource = timeSource;
        this.meterRegistry = meterRegistry;
    }

    public RateLimitDecision check(String tenantId, String routePath, String clientKey, long permits) {
        Optional<RateLimitPolicyEntity> resolvedPolicy = resolvePolicy(tenantId, routePath);

        if (resolvedPolicy.isPresent() && !resolvedPolicy.get().isEnabled()) {
            RateLimitPolicyEntity disabled = resolvedPolicy.get();
            return RateLimitDecision.allow(
                    disabled.getCapacity(),
                    disabled.getCapacity(),
                    0L,
                    timeSource.epochSeconds(),
                    disabled.getAlgorithm()
            );
        }

        String policyId = resolvedPolicy.map(RateLimitPolicyEntity::getId).orElse("default");
        RateLimitConfig config = resolvedPolicy.map(RateLimitPolicyEntity::toConfig).orElse(FALLBACK_CONFIG);
        String fingerprint = resolvedPolicy.map(RateLimitPolicyEntity::configFingerprint).orElse("default");
        String bucketKey = tenantId + "|" + resolvedPolicy.map(RateLimitPolicyEntity::getRoutePattern).orElse(WILDCARD_ROUTE) + "|" + clientKey;

        BucketEntry entry = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || !existing.fingerprint().equals(fingerprint)) {
                return new BucketEntry(fingerprint, RateLimiterFactory.create(config, timeSource));
            }
            return existing;
        });

        RateLimitDecision decision = entry.limiter().tryAcquire(permits);
        recordMetrics(tenantId, decision);

        if (!decision.allowed()) {
            auditLogRepository.save(new RateLimitAuditLogEntity(
                    policyId,
                    tenantId,
                    routePath,
                    clientKey,
                    decision.algorithm(),
                    permits,
                    false,
                    decision.remaining(),
                    decision.retryAfterSeconds(),
                    Instant.ofEpochSecond(timeSource.epochSeconds())
            ));
        }

        return decision;
    }

    public List<RateLimitAuditLogEntity> recentRejections(String tenantId) {
        return auditLogRepository.findTop50ByTenantIdOrderByOccurredAtDesc(tenantId);
    }

    private Optional<RateLimitPolicyEntity> resolvePolicy(String tenantId, String routePath) {
        return policyRepository.findByTenantIdAndRoutePattern(tenantId, routePath)
                .or(() -> policyRepository.findByTenantIdAndRoutePattern(tenantId, WILDCARD_ROUTE));
    }

    private void recordMetrics(String tenantId, RateLimitDecision decision) {
        meterRegistry.counter(
                "aegis.ratelimit.decisions",
                "tenant", tenantId,
                "algorithm", decision.algorithm().name(),
                "outcome", decision.allowed() ? "allowed" : "rejected"
        ).increment();
    }

    private record BucketEntry(String fingerprint, RateLimiter limiter) {
    }
}
