package com.mickaelgeronimo.aegis.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mickaelgeronimo.aegis.core.RateLimitDecision;
import com.mickaelgeronimo.aegis.service.RateLimitEngineService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP filter enforcing live rate-limit headers and 429 throttling on protected demo routes.
 */
@Component
public class RateLimitGatewayFilter extends OncePerRequestFilter {

    private final RateLimitEngineService engineService;
    private final ObjectMapper objectMapper;

    public RateLimitGatewayFilter(RateLimitEngineService engineService, ObjectMapper objectMapper) {
        this.engineService = engineService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/demo/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String tenantId = headerOrDefault(request, "X-Tenant-Id", "public");
        String clientKey = headerOrDefault(request, "X-Client-Key", request.getRemoteAddr());

        RateLimitDecision decision = engineService.check(tenantId, request.getRequestURI(), clientKey, 1L);
        RateLimitHeaders.writeToServletResponse(decision, response);

        if (!decision.allowed()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Retry after " + decision.retryAfterSeconds() + " seconds."
            );
            problem.setTitle("Too Many Requests");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), problem);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String headerOrDefault(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
