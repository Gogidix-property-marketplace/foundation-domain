package com.gogidix.infrastructure.ratelimit.application.service;

import com.gogidix.infrastructure.ratelimit.domain.dto.RateLimitCheckRequest;
import com.gogidix.infrastructure.ratelimit.domain.dto.RateLimitCheckResponse;
import com.gogidix.infrastructure.ratelimit.domain.dto.RateLimitPolicyDto;

import java.util.List;

/**
 * Service interface for rate limiting operations.
 *
 * Provides enterprise-grade rate limiting with:
 * - Multiple algorithm support (Token Bucket, Sliding Window, Fixed Window, Leaky Bucket)
 * - Redis-based distributed rate limiting
 * - Real-time monitoring and analytics
 * - Multi-tenant isolation
 * - Sub-millisecond latency checks
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
public interface RateLimitService {

    /**
     * Check if a request should be allowed based on rate limiting policy.
     *
     * @param request The rate limit check request
     * @return Response indicating if request is allowed and remaining quota
     */
    RateLimitCheckResponse checkRateLimit(RateLimitCheckRequest request);

    /**
     * Create or update a rate limit policy.
     *
     * @param policyDto The rate limit policy data
     * @return Created/updated policy
     */
    RateLimitPolicyDto createOrUpdatePolicy(RateLimitPolicyDto policyDto);

    /**
     * Get rate limit policy by ID.
     *
     * @param id Policy ID
     * @return Rate limit policy
     */
    RateLimitPolicyDto getPolicy(String id);

    /**
     * Get all policies for a tenant with pagination.
     *
     * @param tenantId Tenant ID
     * @param page Page number
     * @param size Page size
     * @return List of policies
     */
    List<RateLimitPolicyDto> getPoliciesByTenant(String tenantId, int page, int size);

    /**
     * Delete a rate limit policy.
     *
     * @param id Policy ID
     * @param tenantId Tenant ID for authorization
     */
    void deletePolicy(String id, String tenantId);

    /**
     * Get current usage statistics for a client.
     *
     * @param clientId Client ID
     * @param endpoint Endpoint
     * @return Usage statistics
     */
    Object getUsageStats(String clientId, String endpoint);

    /**
     * Reset rate limit counters for a client.
     *
     * @param clientId Client ID
     * @param endpoint Endpoint (optional)
     * @param tenantId Tenant ID for authorization
     */
    void resetCounters(String clientId, String endpoint, String tenantId);

    /**
     * Get real-time metrics for monitoring.
     *
     * @return Rate limiting metrics
     */
    Object getMetrics();

    /**
     * Bulk import rate limit policies.
     *
     * @param policies List of policies to import
     * @param tenantId Tenant ID
     * @return Import results
     */
    Object bulkImportPolicies(List<RateLimitPolicyDto> policies, String tenantId);

    /**
     * Validate API key and return associated policy.
     *
     * @param apiKey API key to validate
     * @return Associated rate limit policy if valid
     */
    RateLimitPolicyDto validateAndGetPolicy(String apiKey);
}