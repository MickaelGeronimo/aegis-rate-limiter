package com.mickaelgeronimo.aegis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Aegis Multi-Algorithm Rate Limiter & Quota Engine.
 */
@SpringBootApplication
public class AegisRateLimiterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisRateLimiterApplication.class, args);
    }
}
