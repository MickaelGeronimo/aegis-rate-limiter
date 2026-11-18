package com.mickaelgeronimo.aegis.service;

import com.mickaelgeronimo.aegis.core.RateLimitAlgorithmType;
import com.mickaelgeronimo.aegis.core.RateLimitDecision;
import com.mickaelgeronimo.aegis.domain.RateLimitAuditLogEntity;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyEntity;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RateLimitEngineServiceTest {

    @Autowired
    private RateLimitPolicyService policyService;

    @Autowired
    private RateLimitEngineService engineService;

    @Autowired
    private RateLimitPolicyRepository policyRepository;

    @BeforeEach
    void cleanup() {
        policyRepository.deleteAll();
    }

    @Test
    void resolvesExactRouteBeforeWildcardAndReplacesBucketsWhenPolicyChanges() {
        RateLimitPolicyEntity wildcard = policyService.createPolicy(
                "tenant-svc", "/**", RateLimitAlgorithmType.FIXED_WINDOW_COUNTER, 10, 10, 60, true);
        RateLimitPolicyEntity exact = policyService.createPolicy(
                "tenant-svc", "/orders", RateLimitAlgorithmType.TOKEN_BUCKET, 1, 1, 60, true);

        assertThat(wildcard.getId()).isNotBlank();
        assertThat(engineService.check("tenant-svc", "/orders", "user-1", 1).allowed()).isTrue();
        RateLimitDecision blocked = engineService.check("tenant-svc", "/orders", "user-1", 1);
        assertThat(blocked.allowed()).isFalse();

        // Fallback to wildcard policy on unmapped route
        RateLimitDecision otherRoute = engineService.check("tenant-svc", "/catalog", "user-1", 1);
        assertThat(otherRoute.allowed()).isTrue();
        assertThat(otherRoute.limit()).isEqualTo(10L);

        // Updating capacity creates a fresh bucket under the new fingerprint
        policyService.updatePolicy(exact.getId(), RateLimitAlgorithmType.TOKEN_BUCKET, 3, 3, 60, true);
        RateLimitDecision afterUpdate = engineService.check("tenant-svc", "/orders", "user-1", 1);
        assertThat(afterUpdate.allowed()).isTrue();
        assertThat(afterUpdate.limit()).isEqualTo(3L);

        List<RateLimitAuditLogEntity> rejections = engineService.recentRejections("tenant-svc");
        assertThat(rejections).hasSize(1);
        assertThat(rejections.get(0).getRoutePath()).isEqualTo("/orders");
    }
}
