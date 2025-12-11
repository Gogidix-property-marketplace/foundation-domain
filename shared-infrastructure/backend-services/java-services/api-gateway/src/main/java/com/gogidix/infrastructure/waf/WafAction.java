package com.gogidix.infrastructure.waf;

/**
 * WAF Action Enumeration
 * Defines possible actions for WAF rule evaluation
 */
public enum WafAction {
    ALLOW("Allow the request"),
    BLOCK("Block the request"),
    RATE_LIMIT("Apply rate limiting"),
    CHALLENGE("Present challenge to user"),
    LOG("Log the request for monitoring");

    private final String description;

    WafAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
