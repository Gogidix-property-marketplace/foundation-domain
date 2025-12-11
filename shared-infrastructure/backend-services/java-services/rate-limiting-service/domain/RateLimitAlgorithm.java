package com.gogidix.infrastructure.ratelimit.domain;

/**
 * Rate limiting algorithms supported by the service.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
public enum RateLimitAlgorithm {
    /**
     * Token Bucket Algorithm
     * Allows burst traffic up to bucket capacity
     * Tokens are refilled at a constant rate
     * Best for: APIs with occasional burst requirements
     */
    TOKEN_BUCKET("Token Bucket", "Allows burst traffic with sustained rate control"),

    /**
     * Sliding Window Algorithm
     * Counts requests within a sliding time window
     * More accurate than fixed window
     * Best for: APIs requiring precise rate control
     */
    SLIDING_WINDOW("Sliding Window", "Counts requests in a sliding time window"),

    /**
     * Fixed Window Algorithm
     * Resets counter at fixed intervals
     * Simple and performant
     * Best for: Basic rate limiting requirements
     */
    FIXED_WINDOW("Fixed Window", "Simple counter-based rate limiting with fixed reset"),

    /**
     * Leaky Bucket Algorithm
     * Smooths traffic to constant rate
     * Processes requests at steady rate
     * Best for: Traffic shaping and load balancing
     */
    LEAKY_BUCKET("Leaky Bucket", "Smooths traffic output at constant rate");

    private final String displayName;
    private final String description;

    RateLimitAlgorithm(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}