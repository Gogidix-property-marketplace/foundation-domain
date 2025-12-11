package com.gogidix.infrastructure.gateway.security.waf;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * WAF Evaluation Result.
 *
 * Represents the result of a WAF evaluation for a request.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
public class WafResult {

    /**
     * Whether the request is allowed
     */
    private boolean allowed;

    /**
     * Reason for the decision
     */
    private String reason;

    /**
     * Action taken
     */
    private WafAction action;

    /**
     * Risk score (1-10)
     */
    private int riskScore;

    /**
     * Additional details
     */
    private Map<String, Object> details;

    /**
     * Time taken for evaluation (ms)
     */
    private long evaluationTimeMs;

    /**
     * Rule ID that triggered the action
     */
    private String ruleId;

    /**
     * Request ID for tracking
     */
    private String requestId;

    /**
     * Client IP
     */
    private String clientIp;

    /**
     * Timestamp of evaluation
     */
    private long timestamp;

    /**
     * Whether this should be logged
     */
    private boolean shouldLog;

    /**
     * Block duration in seconds (if applicable)
     */
    private Integer blockDurationSeconds;
}