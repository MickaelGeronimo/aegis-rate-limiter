package com.mickaelgeronimo.aegis.api;

import com.mickaelgeronimo.aegis.core.RateLimitDecision;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.util.function.BiConsumer;

/**
 * Shared helper emitting standardized HTTP rate-limit headers consistently across controllers and filters.
 */
public final class RateLimitHeaders {

    public static final String LIMIT = "X-RateLimit-Limit";
    public static final String REMAINING = "X-RateLimit-Remaining";
    public static final String RESET = "X-RateLimit-Reset";
    public static final String ALGORITHM = "X-RateLimit-Algorithm";
    public static final String RETRY_AFTER = "Retry-After";

    private RateLimitHeaders() {
    }

    public static HttpHeaders toHttpHeaders(RateLimitDecision decision) {
        HttpHeaders headers = new HttpHeaders();
        apply(decision, headers::set);
        return headers;
    }

    public static void writeToServletResponse(RateLimitDecision decision, HttpServletResponse response) {
        apply(decision, response::setHeader);
    }

    private static void apply(RateLimitDecision decision, BiConsumer<String, String> headerSetter) {
        headerSetter.accept(LIMIT, String.valueOf(decision.limit()));
        headerSetter.accept(REMAINING, String.valueOf(decision.remaining()));
        headerSetter.accept(RESET, String.valueOf(decision.resetEpochSeconds()));
        headerSetter.accept(ALGORITHM, decision.algorithm().name());
        if (!decision.allowed()) {
            headerSetter.accept(RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        }
    }
}
