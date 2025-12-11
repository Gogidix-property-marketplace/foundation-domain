package com.gogidix.infrastructure.gateway.security.waf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WAF Decision Cache Entry.
 *
 * Caches WAF decisions to improve performance for repeated requests.
 * Helps reduce computational overhead for legitimate requests.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WafDecisionCache {

    /**
     * Cache key (typically hash of request features)
     */
    private String cacheKey;

    /**
     * The WAF decision/result
     */
    private WafResult decision;

    /**
     * When this cache entry was created
     */
    private LocalDateTime createdAt;

    /**
     * When this cache entry expires
     */
    private LocalDateTime expiresAt;

    /**
     * Number of times this cache entry has been hit
     */
    private long hitCount;

    /**
     * Last time this cache entry was accessed
     */
    private LocalDateTime lastAccessed;

    /**
     * Whether this cache entry is still valid
     */
    private boolean isValid;

    /**
     * TTL in seconds
     */
    private int ttlSeconds;

    /**
     * Check if cache entry has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark the cache entry as accessed
     */
    public void markAccessed() {
        this.lastAccessed = LocalDateTime.now();
        this.hitCount++;
    }

    /**
     * Create a new cache entry with default TTL
     */
    public static WafDecisionCache create(String cacheKey, WafResult decision) {
        LocalDateTime now = LocalDateTime.now();
        return WafDecisionCache.builder()
                .cacheKey(cacheKey)
                .decision(decision)
                .createdAt(now)
                .lastAccessed(now)
                .expiresAt(now.plusSeconds(300)) // Default 5 minutes
                .hitCount(1)
                .isValid(true)
                .ttlSeconds(300)
                .build();
    }

    /**
     * Create a new cache entry with custom TTL
     */
    public static WafDecisionCache create(String cacheKey, WafResult decision, int ttlSeconds) {
        LocalDateTime now = LocalDateTime.now();
        return WafDecisionCache.builder()
                .cacheKey(cacheKey)
                .decision(decision)
                .createdAt(now)
                .lastAccessed(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .hitCount(1)
                .isValid(true)
                .ttlSeconds(ttlSeconds)
                .build();
    }
}