package com.mickaelgeronimo.aegis.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mickaelgeronimo.aegis.domain.RateLimitAuditLogRepository;
import com.mickaelgeronimo.aegis.domain.RateLimitPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyAndEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitPolicyRepository policyRepository;

    @Autowired
    private RateLimitAuditLogRepository auditLogRepository;

    @BeforeEach
    void resetDatabase() {
        auditLogRepository.deleteAll();
        policyRepository.deleteAll();
    }

    @Test
    void supportsFullPolicyLifecycleAndProblemDetailsValidation() throws Exception {
        String createBody = """
                {
                  "tenantId": "acme-pay",
                  "routePattern": "/payments",
                  "algorithm": "TOKEN_BUCKET",
                  "capacity": 5,
                  "refillTokens": 5,
                  "windowSeconds": 60,
                  "enabled": true
                }
                """;

        MvcResult createdResult = mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("acme-pay"))
                .andExpect(jsonPath("$.algorithm").value("TOKEN_BUCKET"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createdResult.getResponse().getContentAsString());
        String policyId = createdJson.get("id").asText();
        assertThat(policyId).startsWith("pol_");

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate rate limit policy"));

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "",
                                  "routePattern": "/bad",
                                  "algorithm": "TOKEN_BUCKET",
                                  "capacity": 0,
                                  "refillTokens": 1,
                                  "windowSeconds": 60
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));

        mockMvc.perform(put("/api/v1/policies/{id}", policyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "algorithm": "SLIDING_WINDOW_LOG",
                                  "capacity": 8,
                                  "refillTokens": 8,
                                  "windowSeconds": 30,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("SLIDING_WINDOW_LOG"))
                .andExpect(jsonPath("$.capacity").value(8));

        mockMvc.perform(delete("/api/v1/policies/{id}", policyId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/policies/{id}", policyId))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesQuotaWithStandardHeadersRecordsAuditAndProtectsDemoFilter() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "fintech-api",
                                  "routePattern": "/transfers",
                                  "algorithm": "SLIDING_WINDOW_LOG",
                                  "capacity": 2,
                                  "refillTokens": 2,
                                  "windowSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated());

        String checkBody = """
                {
                  "tenantId": "fintech-api",
                  "routePath": "/transfers",
                  "clientKey": "client-42",
                  "permits": 1
                }
                """;

        mockMvc.perform(post("/api/v1/ratelimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkBody))
                .andExpect(status().isOk())
                .andExpect(header().string(RateLimitHeaders.LIMIT, "2"))
                .andExpect(header().string(RateLimitHeaders.REMAINING, "1"))
                .andExpect(header().string(RateLimitHeaders.ALGORITHM, "SLIDING_WINDOW_LOG"))
                .andExpect(jsonPath("$.allowed").value(true));

        mockMvc.perform(post("/api/v1/ratelimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkBody))
                .andExpect(status().isOk())
                .andExpect(header().string(RateLimitHeaders.REMAINING, "0"))
                .andExpect(jsonPath("$.allowed").value(true));

        mockMvc.perform(post("/api/v1/ratelimit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(RateLimitHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.retryAfterSeconds", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/audit").param("tenantId", "fintech-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientKey").value("client-42"))
                .andExpect(jsonPath("$[0].allowed").value(false));

        // Verify live HTTP gateway filter on /api/v1/demo/ping
        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "demo-tenant",
                                  "routePattern": "/api/v1/demo/ping",
                                  "algorithm": "FIXED_WINDOW_COUNTER",
                                  "capacity": 1,
                                  "refillTokens": 1,
                                  "windowSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/demo/ping")
                        .header("X-Tenant-Id", "demo-tenant")
                        .header("X-Client-Key", "browser-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RateLimitHeaders.LIMIT, "1"))
                .andExpect(header().string(RateLimitHeaders.REMAINING, "0"))
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/api/v1/demo/ping")
                        .header("X-Tenant-Id", "demo-tenant")
                        .header("X-Client-Key", "browser-1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(RateLimitHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }
}
