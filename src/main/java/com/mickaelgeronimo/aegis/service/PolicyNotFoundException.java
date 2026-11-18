package com.mickaelgeronimo.aegis.service;

/**
 * Thrown when a requested rate-limit policy identifier does not exist.
 */
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policyId) {
        super("Rate limit policy not found: " + policyId);
    }
}
