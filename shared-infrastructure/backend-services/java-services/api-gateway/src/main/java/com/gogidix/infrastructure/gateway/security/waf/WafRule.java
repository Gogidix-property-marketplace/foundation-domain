package com.gogidix.infrastructure.gateway.security.waf;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * WAF Rule Configuration.
 *
 * Represents a Web Application Firewall rule for protecting against:
 * - SQL injection attacks
 * - XSS attacks
 * - Path traversal attacks
 * - Command injection
 * - DDoS pattern matching
 * - Custom threat patterns
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WafRule {

    /**
     * Unique identifier for the rule
     */
    private String id;

    /**
     * Rule name
     */
    private String name;

    /**
     * Rule description
     */
    private String description;

    /**
     * Type of WAF rule
     */
    private WafRuleType type;

    /**
     * Priority of rule (lower number = higher priority)
     */
    private Integer priority;

    /**
     * Whether the rule is active
     */
    private Boolean enabled;

    /**
     * Action to take when rule matches
     */
    private WafAction action;

    /**
     * Pattern to match (regex)
     */
    private String pattern;

    /**
     * List of specific patterns for complex rules
     */
    private List<String> patterns;

    /**
     * Request parameters to check
     */
    private List<String> targetParameters;

    /**
     * Request headers to check
     */
    private List<String> targetHeaders;

    /**
     * Request body check flag
     */
    private Boolean checkBody;

    /**
     * Case sensitivity flag
     */
    private Boolean caseSensitive;

    /**
     * Maximum match count per minute
     */
    private Integer maxMatchesPerMinute;

    /**
     * Block duration in seconds
     */
    private Integer blockDurationSeconds;

    /**
     * Risk score (1-10)
     */
    private Integer riskScore;

    /**
     * Tags for rule categorization
     */
    private List<String> tags;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Rule metadata
     */
    private Map<String, Object> metadata;

    /**
     * WAF Rule Types
     */
    public enum WafRuleType {
        SQL_INJECTION("SQL Injection", "Detects SQL injection patterns"),
        XSS("Cross-Site Scripting", "Detects XSS attack patterns"),
        PATH_TRAVERSAL("Path Traversal", "Detects path traversal attempts"),
        COMMAND_INJECTION("Command Injection", "Detects command injection patterns"),
        DDOS_PATTERN("DDoS Pattern", "Detects DDoS attack patterns"),
        RATE_LIMIT("Rate Limit", "Enables rate limiting"),
        IP_WHITELIST("IP Whitelist", "Whitelists specific IP addresses"),
        IP_BLACKLIST("IP Blacklist", "Blacklists specific IP addresses"),
        USER_AGENT_BLOCK("User Agent Block", "Blocks malicious user agents"),
        REQUEST_SIZE_LIMIT("Request Size Limit", "Limits request size"),
        RESPONSE_SIZE_LIMIT("Response Size Limit", "Limits response size"),
        GEOLOCATION_BLOCK("Geolocation Block", "Blocks requests from certain countries"),
        CUSTOM_SIGNATURE("Custom Signature", "Custom pattern matching rule");

        private final String displayName;
        private final String description;

        WafRuleType(String displayName, String description) {
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

    /**
     * WAF Actions
     */
    public enum WafAction {
        ALLOW("Allow", "Allow the request to proceed"),
        BLOCK("Block", "Block the request immediately"),
        RATE_LIMIT("Rate Limit", "Apply rate limiting"),
        REDIRECT("Redirect", "Redirect to another URL"),
        CHALLENGE("Challenge", "Present a challenge/response"),
        LOG("Log Only", "Log the match but take no action"),
        DECOY("Decoy", "Return fake data"),
        TARPIT("Tarpit", "Slow down the connection");

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
         * Convert to the standalone WafAction enum
         */
        public com.gogidix.infrastructure.gateway.security.waf.WafAction toWafAction() {
            try {
                return com.gogidix.infrastructure.gateway.security.waf.WafAction.valueOf(this.name());
            } catch (IllegalArgumentException e) {
                // Default to BLOCK if action not found
                return com.gogidix.infrastructure.gateway.security.waf.WafAction.BLOCK;
            }
        }

        /**
         * Convert from standalone WafAction enum
         */
        public static WafAction fromWafAction(com.gogidix.infrastructure.gateway.security.waf.WafAction wafAction) {
            try {
                return WafAction.valueOf(wafAction.name());
            } catch (IllegalArgumentException e) {
                // Default to BLOCK if action not found
                return BLOCK;
            }
        }
    }
}