package com.gogidix.infrastructure.ratelimit.infrastructure.repository;

import com.gogidix.infrastructure.ratelimit.domain.RateLimitAlgorithm;
import com.gogidix.infrastructure.ratelimit.domain.RateLimitPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Rate Limit Policy entities.
 *
 * Provides optimized queries for rate limit policy lookups and management.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Repository
public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicy, UUID> {

    /**
     * Find active policy by client ID and API key
     */
    @Query("SELECT r FROM RateLimitPolicy r WHERE r.clientId = :clientId AND r.apiKey = :apiKey AND r.isActive = true ORDER BY r.priority DESC")
    Optional<RateLimitPolicy> findActivePolicyByClientAndApiKey(
            @Param("clientId") String clientId,
            @Param("apiKey") String apiKey);

    /**
     * Find active policy by client ID and endpoint
     */
    @Query("SELECT r FROM RateLimitPolicy r WHERE r.clientId = :clientId AND r.endpoint = :endpoint AND r.isActive = true ORDER BY r.priority DESC")
    List<RateLimitPolicy> findActivePoliciesByClientAndEndpoint(
            @Param("clientId") String clientId,
            @Param("endpoint") String endpoint);

    /**
     * Find all active policies for a tenant
     */
    @Query("SELECT r FROM RateLimitPolicy r WHERE r.tenantId = :tenantId AND r.isActive = true ORDER BY r.priority DESC")
    List<RateLimitPolicy> findActivePoliciesByTenant(@Param("tenantId") String tenantId);

    /**
     * Find policies by algorithm type
     */
    List<RateLimitPolicy> findByAlgorithmAndIsActive(RateLimitAlgorithm algorithm, Boolean isActive);

    /**
     * Search policies by client ID or endpoint with pagination
     */
    @Query("SELECT r FROM RateLimitPolicy r WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(r.clientId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.endpoint) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:tenantId IS NULL OR r.tenantId = :tenantId) AND " +
           "(:isActive IS NULL OR r.isActive = :isActive)")
    Page<RateLimitPolicy> searchPolicies(
            @Param("searchTerm") String searchTerm,
            @Param("tenantId") String tenantId,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    /**
     * Count active policies per tenant
     */
    @Query("SELECT r.tenantId, COUNT(r) FROM RateLimitPolicy r WHERE r.isActive = true GROUP BY r.tenantId")
    List<Object[]> countActivePoliciesByTenant();

    /**
     * Check if API key exists
     */
    boolean existsByApiKeyAndIsActive(String apiKey, Boolean isActive);

    /**
     * Find all policies that need refill (for token bucket algorithm)
     */
    @Query("SELECT r FROM RateLimitPolicy r WHERE r.algorithm = 'TOKEN_BUCKET' AND r.isActive = true")
    List<RateLimitPolicy> findTokenBucketPolicies();

    /**
     * Bulk update active status
     */
    @Query("UPDATE RateLimitPolicy r SET r.isActive = :isActive WHERE r.tenantId = :tenantId")
    int updateActiveStatusByTenant(@Param("tenantId") String tenantId, @Param("isActive") Boolean isActive);
}