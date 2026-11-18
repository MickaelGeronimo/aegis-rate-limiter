package com.mickaelgeronimo.aegis.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateLimitAuditLogRepository extends JpaRepository<RateLimitAuditLogEntity, String> {

    List<RateLimitAuditLogEntity> findTop50ByTenantIdOrderByOccurredAtDesc(String tenantId);
}
