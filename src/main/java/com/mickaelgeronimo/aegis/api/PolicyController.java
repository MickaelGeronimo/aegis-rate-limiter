package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.domain.RateLimitPolicyEntity;
import com.mickaelgeronimo.aegis.service.RateLimitPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for managing per-tenant rate-limit quota policies.
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final RateLimitPolicyService policyService;

    public PolicyController(RateLimitPolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> create(@Valid @RequestBody CreatePolicyRequest request) {
        RateLimitPolicyEntity created = policyService.createPolicy(
                request.tenantId(),
                request.routePattern(),
                request.algorithm(),
                request.capacity(),
                request.refillTokens(),
                request.windowSeconds(),
                request.isEnabledOrDefault()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.from(created));
    }

    @GetMapping
    public List<PolicyResponse> list(@RequestParam(required = false) String tenantId) {
        return policyService.listPolicies(tenantId).stream()
                .map(PolicyResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PolicyResponse getById(@PathVariable String id) {
        return PolicyResponse.from(policyService.getPolicy(id));
    }

    @PutMapping("/{id}")
    public PolicyResponse update(@PathVariable String id, @Valid @RequestBody UpdatePolicyRequest request) {
        RateLimitPolicyEntity updated = policyService.updatePolicy(
                id,
                request.algorithm(),
                request.capacity(),
                request.refillTokens(),
                request.windowSeconds(),
                request.enabled()
        );
        return PolicyResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
