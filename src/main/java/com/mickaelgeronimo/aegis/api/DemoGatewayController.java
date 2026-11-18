package com.mickaelgeronimo.aegis.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sample protected endpoint demonstrating live HTTP gateway filter rate limiting.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoGatewayController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "ok",
                "message", "Request admitted by Aegis Rate Limiter"
        );
    }
}
