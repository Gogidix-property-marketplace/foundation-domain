package com.gogidix.infrastructure.gateway.security.waf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * WAF Rule Engine.
 *
 * Evaluates incoming requests against configured WAF rules.
 * Supports complex rule combinations, priorities, and actions.
 *
 * Features:
 * - Rule prioritization
 * - Pattern matching
 * - Custom signatures
 * - Dynamic rule loading
 * - Rule performance metrics
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WafRuleEngine {

    private final ReactiveStringRedisTemplate redisTemplate;

    // In-memory rule cache for high performance
    private final Map<String, WafRule> rulesCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRuleUpdate = new ConcurrentHashMap<>();

    // Precompiled patterns for performance
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    /**
     * Evaluates request against all WAF rules
     */
    public WafResult evaluateRequest(ServerWebExchange exchange) {
        long startTime = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        String clientIp = getClientIp(exchange);

        log.debug("Evaluating WAF rules for request: {} from IP: {}", requestId, clientIp);

        // Load rules if cache is empty or expired
        loadRulesIfNeeded();

        // Sort rules by priority
        List<WafRule> sortedRules = rulesCache.values().stream()
                .filter(WafRule::getEnabled)
                .sorted(Comparator.comparingInt(WafRule::getPriority))
                .collect(Collectors.toList());

        for (WafRule rule : sortedRules) {
            WafResult result = evaluateRule(exchange, rule, requestId, clientIp);
            if (!result.isAllowed()) {
                // Rule triggered - take action
                log.warn("WAF rule triggered: {} for IP: {} - Reason: {}",
                        rule.getName(), clientIp, rule.getDescription());

                // Record rule violation
                recordRuleViolation(clientIp, rule.getId());

                return result.toBuilder()
                        .evaluationTimeMs((System.nanoTime() - startTime) / 1_000_000)
                        .build();
            }
        }

        // No rules triggered - request is allowed
        return WafResult.builder()
                .allowed(true)
                .reason("NO_RULES_TRIGGERED")
                .action(WafAction.ALLOW)
                .riskScore(1)
                .requestId(requestId)
                .clientIp(clientIp)
                .timestamp(System.currentTimeMillis())
                .evaluationTimeMs((System.nanoTime() - startTime) / 1_000_000)
                .build();
    }

    /**
     * Evaluates a single rule against the request
     */
    private WafResult evaluateRule(ServerWebExchange exchange, WafRule rule, String requestId, String clientIp) {
        long startTime = System.nanoTime();

        try {
            // Check rule type and evaluate accordingly
            boolean ruleMatches = switch (rule.getType()) {
                case SQL_INJECTION -> checkSqlInjection(exchange, rule);
                case XSS -> checkXss(exchange, rule);
                case PATH_TRAVERSAL -> checkPathTraversal(exchange, rule);
                case COMMAND_INJECTION -> checkCommandInjection(exchange, rule);
                case IP_BLACKLIST -> checkIpBlacklist(exchange, rule);
                case IP_WHITELIST -> checkIpWhitelist(exchange, rule);
                case RATE_LIMIT -> checkRateLimit(exchange, rule, clientIp);
                case USER_AGENT_BLOCK -> checkUserAgentBlock(exchange, rule);
                case REQUEST_SIZE_LIMIT -> checkRequestSizeLimit(exchange, rule);
                case GEOLOCATION_BLOCK -> checkGeolocationBlock(exchange, rule, clientIp);
                case CUSTOM_SIGNATURE -> checkCustomSignature(exchange, rule);
                default -> false;
            };

            if (ruleMatches) {
                log.debug("WAF rule matched: {} - {}", rule.getName(), rule.getType());

                return WafResult.builder()
                        .allowed(false)
                        .reason("RULE_" + rule.getType().name())
                        .action(rule.getAction().toWafAction())
                        .riskScore(rule.getRiskScore())
                        .ruleId(rule.getId())
                        .requestId(requestId)
                        .clientIp(clientIp)
                        .timestamp(System.currentTimeMillis())
                        .evaluationTimeMs((System.nanoTime() - startTime) / 1_000_000)
                        .details(Map.of(
                                "ruleName", rule.getName(),
                                "ruleType", rule.getType(),
                                "description", rule.getDescription()
                        ))
                        .blockDurationSeconds(rule.getBlockDurationSeconds())
                        .shouldLog(true)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error evaluating WAF rule: {}", rule.getId(), e);
            // On error, be conservative and allow request but log the issue
        }

        return WafResult.builder()
                .allowed(true)
                .build();
    }

    /**
     * Checks for SQL injection patterns
     */
    private boolean checkSqlInjection(ServerWebExchange exchange, WafRule rule) {
        String content = extractRequestContent(exchange, rule);
        return containsPattern(content, rule.getPatterns());
    }

    /**
     * Checks for XSS patterns
     */
    private boolean checkXss(ServerWebExchange exchange, WafRule rule) {
        String content = extractRequestContent(exchange, rule);
        return containsPattern(content, rule.getPatterns());
    }

    /**
     * Checks for path traversal patterns
     */
    private boolean checkPathTraversal(ServerWebExchange exchange, WafRule rule) {
        String path = exchange.getRequest().getURI().getPath();
        String queryString = exchange.getRequest().getURI().getQuery();

        String content = path + " " + (queryString != null ? queryString : "");
        return containsPattern(content, rule.getPatterns());
    }

    /**
     * Checks for command injection patterns
     */
    private boolean checkCommandInjection(ServerWebExchange exchange, WafRule rule) {
        String content = extractRequestContent(exchange, rule);
        return containsPattern(content, rule.getPatterns());
    }

    /**
     * Checks IP against blacklist
     */
    private boolean checkIpBlacklist(ServerWebExchange exchange, WafRule rule) {
        String clientIp = getClientIp(exchange);
        return rule.getPatterns().contains(clientIp);
    }

    /**
     * Checks IP against whitelist
     */
    private boolean checkIpWhitelist(ServerWebExchange exchange, WafRule rule) {
        String clientIp = getClientIp(exchange);
        return !rule.getPatterns().contains(clientIp);
    }

    /**
     * Checks rate limit
     */
    private boolean checkRateLimit(ServerWebExchange exchange, WafRule rule, String clientIp) {
        String key = String.format("waf:rate:%s:%s", rule.getId(), clientIp);

        // Use Redis for distributed rate limiting
        return redisTemplate.opsForValue()
                .increment(key)
                .map(count -> {
                    if (count == 1) {
                        // Set expiration on first increment
                        redisTemplate.expire(key, Duration.ofSeconds(rule.getMaxMatchesPerMinute()))
                                .subscribe();
                    }
                    return count > rule.getMaxMatchesPerMinute();
                })
                .block(Duration.ofMillis(100));
    }

    /**
     * Checks user agent against blocked list
     */
    private boolean checkUserAgentBlock(ServerWebExchange exchange, WafRule rule) {
        String userAgent = exchange.getRequest()
                .getHeaders()
                .getFirst("User-Agent");

        if (userAgent != null && !userAgent.isEmpty()) {
            return containsPattern(userAgent, rule.getPatterns());
        }

        return false;
    }

    /**
     * Checks request size limit
     */
    private boolean checkRequestSizeLimit(ServerWebExchange exchange, WafRule rule) {
        String contentLength = exchange.getRequest()
                .getHeaders()
                .getFirst("Content-Length");

        if (contentLength != null) {
            try {
                long size = Long.parseLong(contentLength);
                // Assume max size is stored in rule metadata
                Object maxSize = rule.getMetadata() != null ?
                        rule.getMetadata().get("maxSizeBytes") : null;
                if (maxSize != null) {
                    return size > ((Number) maxSize).longValue();
                }
            } catch (NumberFormatException e) {
                // Invalid Content-Length header
                return true;
            }
        }

        return false;
    }

    /**
     * Checks geolocation block
     */
    private boolean checkGeolocationBlock(ServerWebExchange exchange, WafRule rule, String clientIp) {
        // Simplified geolocation check
        String country = getCountryFromIp(clientIp);
        return rule.getPatterns().contains(country);
    }

    /**
     * Checks custom signature patterns
     */
    private boolean checkCustomSignature(ServerWebExchange exchange, WafRule rule) {
        String content = extractRequestContent(exchange, rule);
        return containsPattern(content, rule.getPatterns());
    }

    /**
     * Extracts request content for pattern matching
     */
    private String extractRequestContent(ServerWebExchange exchange, WafRule rule) {
        StringBuilder content = new StringBuilder();

        // Add URL and query parameters
        content.append(exchange.getRequest().getURI().toString()).append(" ");

        // Add headers if specified
        if (rule.getTargetHeaders() != null && !rule.getTargetHeaders().isEmpty()) {
            for (String headerName : rule.getTargetHeaders()) {
                String headerValue = exchange.getRequest()
                        .getHeaders()
                        .getFirst(headerName);
                if (headerValue != null) {
                    content.append(headerName).append(":").append(headerValue).append(" ");
                }
            }
        }

        // Add parameters if specified
        if (rule.getTargetParameters() != null && !rule.getTargetParameters().isEmpty()) {
            for (String paramName : rule.getTargetParameters()) {
                String paramValue = exchange.getRequest()
                        .getQueryParams()
                        .getFirst(paramName);
                if (paramValue != null) {
                    content.append(paramName).append("=").append(paramValue).append(" ");
                }
            }
        }

        return content.toString();
    }

    /**
     * Checks if content contains any of the specified patterns
     */
    private boolean containsPattern(String content, List<String> patterns) {
        if (content == null || content.isEmpty() || patterns == null || patterns.isEmpty()) {
            return false;
        }

        for (String patternStr : patterns) {
            Pattern pattern = compiledPatterns.computeIfAbsent(patternStr, p -> {
                try {
                    return Pattern.compile(p, Pattern.CASE_INSENSITIVE);
                } catch (Exception e) {
                    log.warn("Invalid regex pattern: {}", p);
                    return null;
                }
            });

            if (pattern != null && pattern.matcher(content).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Records rule violation
     */
    private void recordRuleViolation(String clientIp, String ruleId) {
        String key = String.format("waf:violations:%s:%s", clientIp, ruleId);
        redisTemplate.opsForValue()
                .increment(key)
                .and(redisTemplate.expire(key, Duration.ofHours(24)))
                .subscribe();
    }

    /**
     * Loads rules from storage if needed
     */
    private void loadRulesIfNeeded() {
        // In production, load from database or configuration store
        // For now, load default rules
        if (rulesCache.isEmpty()) {
            loadDefaultRules();
        }
    }

    /**
     * Loads default WAF rules
     */
    private void loadDefaultRules() {
        // SQL Injection rules
        addRule(WafRule.builder()
                .id("sql_injection_001")
                .name("SQL Injection Detection")
                .description("Detects common SQL injection patterns")
                .type(WafRule.WafRuleType.SQL_INJECTION)
                .priority(100)
                .enabled(true)
                .action(WafRule.WafAction.BLOCK)
                .riskScore(9)
                .patterns(Arrays.asList(
                        "(?i)(union|select|insert|update|delete|drop|create|alter)",
                        "(?i)(\\bor\\b|and\\b|\\bwhere\\b|=|<|>|'|\"|;|--)",
                        "(?i)(sleep|waitfor|delay|pg_sleep|benchmark)"
                ))
                .checkBody(true)
                .targetParameters(Arrays.asList("id", "query", "search"))
                .build());

        // XSS rules
        addRule(WafRule.builder()
                .id("xss_001")
                .name("XSS Detection")
                .description("Detects cross-site scripting patterns")
                .type(WafRule.WafRuleType.XSS)
                .priority(100)
                .enabled(true)
                .action(WafRule.WafAction.BLOCK)
                .riskScore(8)
                .patterns(Arrays.asList(
                        "(?i)<script[^>]*>.*?</script>",
                        "(?i)javascript\\s*:",
                        "(?i)vbscript\\s*:",
                        "(?i)on\\w+\\s*="
                ))
                .checkBody(true)
                .build());

        // Path traversal rules
        addRule(WafRule.builder()
                .id("path_traversal_001")
                .name("Path Traversal Detection")
                .description("Detects path traversal attempts")
                .type(WafRule.WafRuleType.PATH_TRAVERSAL)
                .priority(100)
                .enabled(true)
                .action(WafRule.WafAction.BLOCK)
                .riskScore(9)
                .patterns(Arrays.asList(
                        "\\.\\.[\\\\/]",
                        "(?i)(%2e%2e|\\.\\./)",
                        "(?i)(/etc/passwd|/etc/shadow)",
                        "(?i)(\\\\|/|\\.\\./)"
                ))
                .build());

        // Rate limiting rule
        addRule(WafRule.builder()
                .id("rate_limit_001")
                .name("General Rate Limit")
                .description("Limits requests per IP")
                .type(WafRule.WafRuleType.RATE_LIMIT)
                .priority(200)
                .enabled(true)
                .action(WafRule.WafAction.RATE_LIMIT)
                .riskScore(5)
                .maxMatchesPerMinute(60)
                .blockDurationSeconds(60)
                .build());

        log.info("Loaded {} default WAF rules", rulesCache.size());
    }

    /**
     * Adds a rule to cache
     */
    private void addRule(WafRule rule) {
        rulesCache.put(rule.getId(), rule);
        lastRuleUpdate.put(rule.getId(), System.currentTimeMillis());
    }

    /**
     * Extracts client IP from request
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                "unknown";
    }

    /**
     * Gets country from IP (simplified)
     */
    private String getCountryFromIp(String ipAddress) {
        // Simplified mock implementation
        if (ipAddress.startsWith("8.8.") || ipAddress.startsWith("1.1.")) {
            return "US";
        } else if (ipAddress.startsWith("91.")) {
            return "RU";
        }
        return "UNKNOWN";
    }

    /**
     * Clears rule cache
     */
    public void clearCache() {
        rulesCache.clear();
        lastRuleUpdate.clear();
        compiledPatterns.clear();
        log.info("WAF rule cache cleared");
    }

    /**
     * Gets rule engine statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRules", rulesCache.size());
        stats.put("enabledRules", rulesCache.values().stream()
                .mapToInt(r -> r.getEnabled() ? 1 : 0)
                .sum());
        stats.put("compiledPatterns", compiledPatterns.size());

        Map<String, Long> ruleTypeCounts = rulesCache.values().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getType().name(),
                        Collectors.counting()
                ));
        stats.put("rulesByType", ruleTypeCounts);

        return stats;
    }
}