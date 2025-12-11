package com.gogidix.infrastructure.gateway.security.waf;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP Reputation Service.
 *
 * Maintains reputation scores for IP addresses based on:
 * - Historical behavior
 * - Known threat intelligence feeds
 * - Geolocation data
 * - ASN information
 * - Previous violations
 *
 * Score range: 0-100
 * - 0-20: Malicious (Block)
 * - 21-40: Suspicious (Monitor)
 * - 41-70: Unknown (Evaluate)
 * - 71-100: Clean (Allow)
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpReputationService {

    private final ReactiveStringRedisTemplate redisTemplate;

    // In-memory cache for fast lookups
    private final Map<String, IpReputation> reputationCache = new ConcurrentHashMap<>();

    // Known malicious IP ranges (CIDR notation)
    private static final List<String> MALICIOUS_RANGES = List.of(
            "192.168.0.0/16", // Example private ranges to block
            "10.0.0.0/8",
            "172.16.0.0/12"
    );

    // Known attack patterns
    private static final Map<String, Integer> ATTACK_PATTERNS = Map.of(
            "sql_injection", -20,
            "xss", -15,
            "path_traversal", -25,
            "command_injection", -30,
            "ddos", -20,
            "brute_force", -15
    );

    /**
     * Checks IP reputation
     */
    public IpReputation checkIpReputation(String ipAddress) {
        // Check cache first
        IpReputation cached = reputationCache.get(ipAddress);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        // Calculate reputation score
        IpReputation reputation = calculateReputation(ipAddress);

        // Cache the result
        reputationCache.put(ipAddress, reputation);

        return reputation;
    }

    /**
     * Calculates reputation score for an IP
     */
    private IpReputation calculateReputation(String ipAddress) {
        long startTime = System.currentTimeMillis();

        IpReputation.IpReputationBuilder builder = IpReputation.builder()
                .ipAddress(ipAddress)
                .checkedAt(LocalDateTime.now());

        int score = 50; // Start with neutral score

        // Check Redis for stored reputation
        String key = "ip:reputation:" + ipAddress;
        Mono<String> storedScore = redisTemplate.opsForValue().get(key);
        String scoreStr = storedScore.block(Duration.ofMillis(100));

        if (scoreStr != null) {
            try {
                score = Integer.parseInt(scoreStr);
                log.debug("Found cached reputation for {}: {}", ipAddress, score);
            } catch (NumberFormatException e) {
                log.warn("Invalid reputation score for IP {}: {}", ipAddress, scoreStr);
            }
        } else {
            // Perform fresh analysis
            score = performIpAnalysis(ipAddress);

            // Store in Redis with TTL
            redisTemplate.opsForValue()
                    .set(key, String.valueOf(score), Duration.ofHours(1))
                    .subscribe();
        }

        builder.score(score);

        // Determine risk level
        String riskLevel;
        if (score <= 20) {
            riskLevel = "MALICIOUS";
        } else if (score <= 40) {
            riskLevel = "SUSPICIOUS";
        } else if (score <= 70) {
            riskLevel = "UNKNOWN";
        } else {
            riskLevel = "CLEAN";
        }

        builder.riskLevel(riskLevel);

        // Add details
        Map<String, Object> details = new HashMap<>();
        details.put("checkedAt", LocalDateTime.now());
        details.put("evaluationTimeMs", System.currentTimeMillis() - startTime);
        builder.details(details);

        IpReputation reputation = builder.build();

        log.debug("IP reputation for {}: {} ({})", ipAddress, score, riskLevel);

        return reputation;
    }

    /**
     * Performs detailed IP analysis
     */
    private int performIpAnalysis(String ipAddress) {
        int score = 50;

        // Check against known malicious ranges
        if (isInMaliciousRange(ipAddress)) {
            score -= 50;
            log.warn("IP {} is in known malicious range", ipAddress);
        }

        // Check if it's a private IP
        if (isPrivateIp(ipAddress)) {
            score -= 10;
        }

        // Check for recent violations
        String violationsKey = "ip:violations:" + ipAddress;
        Mono<String> violations = redisTemplate.opsForValue().get(violationsKey);
        String violationsStr = violations.block(Duration.ofMillis(100));

        if (violationsStr != null) {
            try {
                int violationCount = Integer.parseInt(violationsStr);
                score -= Math.min(violationCount * 5, 40);
                log.debug("IP {} has {} violations", ipAddress, violationCount);
            } catch (NumberFormatException e) {
                log.warn("Invalid violation count for IP {}: {}", ipAddress, violationsStr);
            }
        }

        // Check geolocation (simplified)
        String country = getCountryFromIp(ipAddress);
        if (country != null) {
            // Adjust score based on country reputation
            score += getCountryRiskScore(country);
        }

        // Ensure score is within bounds
        score = Math.max(0, Math.min(100, score));

        return score;
    }

    /**
     * Updates reputation based on violation
     */
    public void reportViolation(String ipAddress, String violationType) {
        String violationsKey = "ip:violations:" + ipAddress;

        // Increment violation count
        redisTemplate.opsForValue()
                .increment(violationsKey)
                .doOnSuccess(v -> log.info("Reported {} violation for IP: {}", violationType, ipAddress))
                .subscribe();

        // Set TTL on violations
        redisTemplate.expire(violationsKey, Duration.ofDays(30))
                .subscribe();

        // Get current reputation and reduce it
        String reputationKey = "ip:reputation:" + ipAddress;
        redisTemplate.opsForValue()
                .get(reputationKey)
                .defaultIfEmpty("50")
                .flatMap(currentScore -> {
                    int newScore = Integer.parseInt(currentScore) + ATTACK_PATTERNS.getOrDefault(violationType, -10);
                    newScore = Math.max(0, Math.min(100, newScore));
                    return redisTemplate.opsForValue().set(reputationKey, String.valueOf(newScore), Duration.ofHours(24));
                })
                .subscribe();

        // Invalidate cache
        reputationCache.remove(ipAddress);
    }

    /**
     * Whitelists an IP address
     */
    public void whitelistIp(String ipAddress) {
        String key = "ip:reputation:" + ipAddress;
        redisTemplate.opsForValue()
                .set(key, "100", Duration.ofDays(30))
                .doOnSuccess(v -> log.info("Whitelisted IP: {}", ipAddress))
                .subscribe();

        reputationCache.remove(ipAddress);
    }

    /**
     * Blacklists an IP address
     */
    public void blacklistIp(String ipAddress) {
        String key = "ip:reputation:" + ipAddress;
        redisTemplate.opsForValue()
                .set(key, "0", Duration.ofDays(30))
                .doOnSuccess(v -> log.info("Blacklisted IP: {}", ipAddress))
                .subscribe();

        reputationCache.remove(ipAddress);
    }

    /**
     * Checks if IP is in known malicious range
     */
    private boolean isInMaliciousRange(String ipAddress) {
        // Simplified check - in production, use proper CIDR matching library
        return MALICIOUS_RANGES.stream().anyMatch(range -> ipMatchesRange(ipAddress, range));
    }

    /**
     * Checks if IP is private
     */
    private boolean isPrivateIp(String ipAddress) {
        return ipAddress.startsWith("10.") ||
               ipAddress.startsWith("192.168.") ||
               ipAddress.startsWith("172.") ||
               ipAddress.equals("127.0.0.1") ||
               ipAddress.equals("localhost");
    }

    /**
     * Gets country from IP (simplified mock implementation)
     */
    private String getCountryFromIp(String ipAddress) {
        // In production, use proper GeoIP database like MaxMind
        // For now, return some mock values based on IP patterns
        if (ipAddress.startsWith("8.8.") || ipAddress.startsWith("1.1.")) {
            return "US";
        } else if (ipAddress.startsWith("91.")) {
            return "RU";
        } else if (ipAddress.startsWith("118.")) {
            return "CN";
        }
        return null;
    }

    /**
     * Gets country risk score
     */
    private int getCountryRiskScore(String country) {
        // Simplified country risk scoring
        switch (country) {
            case "US": return 10;
            case "UK": return 8;
            case "DE": return 7;
            case "JP": return 6;
            case "RU": return -10;
            case "CN": return -5;
            case "KP": return -20;
            default: return 0;
        }
    }

    /**
     * Simplified IP range matching
     */
    private boolean ipMatchesRange(String ip, String cidr) {
        // In production, use proper CIDR matching library
        // For now, simple prefix matching
        String[] parts = cidr.split("/");
        String network = parts[0];
        int prefixLength = Integer.parseInt(parts[1]);

        String[] ipParts = ip.split("\\.");
        String[] networkParts = network.split("\\.");

        int bytesToCheck = prefixLength / 8;
        if (bytesToCheck > 4) bytesToCheck = 4;

        for (int i = 0; i < bytesToCheck; i++) {
            if (!ipParts[i].equals(networkParts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Clears the reputation cache
     */
    public void clearCache() {
        reputationCache.clear();
        log.info("IP reputation cache cleared");
    }

    /**
     * Gets reputation statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", reputationCache.size());

        long maliciousCount = reputationCache.values().stream()
                .filter(r -> r.getScore() <= 20)
                .count();
        long suspiciousCount = reputationCache.values().stream()
                .filter(r -> r.getScore() > 20 && r.getScore() <= 40)
                .count();
        long cleanCount = reputationCache.values().stream()
                .filter(r -> r.getScore() > 70)
                .count();

        stats.put("maliciousIps", maliciousCount);
        stats.put("suspiciousIps", suspiciousCount);
        stats.put("cleanIps", cleanCount);

        return stats;
    }

    /**
     * IP Reputation data model
     */
    @Data
    @lombok.Builder
    public static class IpReputation {
        private String ipAddress;
        private int score;
        private String riskLevel;
        private Map<String, Object> details;
        private LocalDateTime checkedAt;
        private boolean whitelisted;
        private boolean blacklisted;

        public boolean isExpired() {
            // Cache expires after 5 minutes
            return checkedAt == null ||
                   checkedAt.plusMinutes(5).isBefore(LocalDateTime.now());
        }
    }
}