package com.mickaelgeronimo.aegis.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicyEntity, String> {

    Optional<RateLimitPolicyEntity> findByTenantIdAndRoutePattern(String tenantId, String routePattern);

    List<RateLimitPolicyEntity> findAllByTenantIdOrderByRoutePatternAsc(String tenantId);

    boolean existsByTenantIdAndRoutePattern(String tenantId, String routePattern);
}
