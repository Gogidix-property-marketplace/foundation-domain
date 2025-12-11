package com.gogidix.infrastructure.gateway.security.waf;

/**
 * WAF Action Enumeration.
 *
 * Defines the possible actions that the WAF can take when a rule is triggered.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
public enum WafAction {

    /**
     * Allow the request to proceed
     */
    ALLOW("Allow", "Request allowed to proceed"),

    /**
     * Block the request immediately
     */
    BLOCK("Block", "Request blocked due to violation"),

    /**
     * Flag the request for monitoring but allow it
     */
    FLAG("Flag", "Request flagged for monitoring"),

    /**
     * Redirect the request to a different URL
     */
    REDIRECT("Redirect", "Request redirected to safe URL"),

    /**
     * Challenge the request with CAPTCHA or similar
     */
    CHALLENGE("Challenge", "Request challenged for verification"),

    /**
     * Rate limit the request
     */
    RATE_LIMIT("Rate Limit", "Request rate limited"),

    /**
     * Log the request for analysis
     */
    LOG("Log", "Request logged for analysis"),

    /**
     * Quarantine the request for review
     */
    QUARANTINE("Quarantine", "Request quarantined for review");

    private final String displayName;
    private final String description;

    WafAction(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this action blocks the request
     */
    public boolean isBlocking() {
        return this == BLOCK || this == QUARANTINE;
    }

    /**
     * Check if this action allows the request
     */
    public boolean isAllowing() {
        return this == ALLOW || this == FLAG || this == LOG;
    }

    /**
     * Check if this action requires user interaction
     */
    public boolean requiresUserInteraction() {
        return this == CHALLENGE;
    }
}