package com.mickaelgeronimo.aegis.service;

/**
 * Thrown when attempting to create a duplicate policy for the same tenant and route pattern.
 */
public class DuplicatePolicyException extends RuntimeException {

    public DuplicatePolicyException(String tenantId, String routePattern) {
        super("Rate limit policy already exists for tenant='" + tenantId + "' and route='" + routePattern + "'");
    }
}
