package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitDecision;
import com.mickaelgeronimo.aegis.service.RateLimitEngineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST enforcement endpoint allowing distributed gateways to evaluate client quotas and inspect rejections.
 */
@RestController
@RequestMapping("/api/v1")
public class RateLimitCheckController {

    private final RateLimitEngineService engineService;

    public RateLimitCheckController(RateLimitEngineService engineService) {
        this.engineService = engineService;
    }

    @PostMapping("/ratelimit/check")
    public ResponseEntity<CheckRateLimitResponse> check(@Valid @RequestBody CheckRateLimitRequest request) {
        RateLimitDecision decision = engineService.check(
                request.tenantId(),
                request.routePath(),
                request.clientKey(),
                request.permitsOrDefault()
        );

        HttpStatus status = decision.allowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status)
                .headers(RateLimitHeaders.toHttpHeaders(decision))
                .body(CheckRateLimitResponse.from(decision));
    }

    @GetMapping("/audit")
    public List<AuditLogResponse> recentRejections(@RequestParam String tenantId) {
        return engineService.recentRejections(tenantId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
