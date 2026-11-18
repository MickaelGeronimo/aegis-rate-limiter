package com.mickaelgeronimo.aegis.service;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.core.TimeSource;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyEntity;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service managing CRUD operations for tenant rate-limit policies.
 */
@Service
public class RateLimitPolicyService {

    private final RateLimitPolicyRepository policyRepository;

    public RateLimitPolicyService(RateLimitPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional
    public RateLimitPolicyEntity createPolicy(
            String tenantId,
            String routePattern,
            RateLimitAlgorithmType algorithm,
            long capacity,
            long refillTokens,
            long windowSeconds,
            boolean enabled
    ) {
        if (policyRepository.existsByTenantIdAndRoutePattern(tenantId, routePattern)) {
            throw new DuplicatePolicyException(tenantId, routePattern);
        }
        RateLimitPolicyEntity entity = new RateLimitPolicyEntity(
                tenantId, routePattern, algorithm, capacity, refillTokens, windowSeconds, enabled);
        return policyRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public RateLimitPolicyEntity getPolicy(String id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<RateLimitPolicyEntity> listPolicies(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return policyRepository.findAll();
        }
        return policyRepository.findAllByTenantIdOrderByRoutePatternAsc(tenantId);
    }

    @Transactional
    public RateLimitPolicyEntity updatePolicy(
            String id,
            RateLimitAlgorithmType algorithm,
            long capacity,
            long refillTokens,
            long windowSeconds,
            boolean enabled
    ) {
        RateLimitPolicyEntity entity = getPolicy(id);
        entity.update(algorithm, capacity, refillTokens, windowSeconds, enabled);
        return policyRepository.save(entity);
    }

    @Transactional
    public void deletePolicy(String id) {
        RateLimitPolicyEntity entity = getPolicy(id);
        policyRepository.delete(entity);
    }

    @Configuration
    static class AegisCoreBeans {
        @Bean
        TimeSource timeSource() {
            return TimeSource.SYSTEM;
        }
    }
}
