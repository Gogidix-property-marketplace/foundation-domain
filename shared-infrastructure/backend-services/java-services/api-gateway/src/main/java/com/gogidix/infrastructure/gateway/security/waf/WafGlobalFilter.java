package com.gogidix.infrastructure.gateway.security.waf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WAF Global Filter for API Gateway.
 *
 * Intercepts all incoming requests and processes them through
 * the Web Application Firewall before forwarding to downstream services.
 *
 * Order: Highest priority filter (executed first)
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class WafGlobalFilter implements GlobalFilter {

    private final WebApplicationFirewall waf;
    private final WafMetricsService metricsService;
    private final WafAlertService alertService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Generate request ID for tracking
        String requestId = UUID.randomUUID().toString();
        request.mutate().header("X-Request-ID", requestId).build();

        log.debug("WAF processing request: {} {} [ID: {}]",
                request.getMethod(), request.getURI(), requestId);

        // Process request through WAF
        return waf.processRequest(exchange)
                .flatMap(wafResult -> {
                    long processingTime = (System.nanoTime() - startTime) / 1_000_000;

                    // Record metrics
                    metricsService.recordRequest(requestId, wafResult, processingTime);

                    if (wafResult.isAllowed()) {
                        log.debug("WAF allowed request: {} [Time: {}ms]", requestId, processingTime);
                        return chain.filter(exchange);
                    } else {
                        return handleBlockedRequest(exchange, wafResult, requestId);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("WAF processing error for request: {}", requestId, throwable);
                    metricsService.recordError(requestId, throwable);
                    return chain.filter(exchange); // Fail open
                });
    }

    /**
     * Handles blocked requests
     */
    private Mono<Void> handleBlockedRequest(ServerWebExchange exchange, WafResult wafResult, String requestId) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Log the blocked request
        log.warn("WAF blocked request: {} {} from {} - Reason: {} [ID: {}]",
                request.getMethod(),
                request.getURI(),
                wafResult.getClientIp(),
                wafResult.getReason(),
                requestId);

        // Send alert for high-risk violations
        if (wafResult.getRiskScore() >= 8) {
            alertService.sendHighRiskAlert(wafResult);
        }

        // Set appropriate response based on action
        switch (wafResult.getAction()) {
            case BLOCK:
                response.setStatusCode(HttpStatus.FORBIDDEN);
                break;
            case RATE_LIMIT:
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                break;
            case CHALLENGE:
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                break;
            case REDIRECT:
                response.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
                // Could redirect to a challenge page
                break;
            default:
                response.setStatusCode(HttpStatus.FORBIDDEN);
        }

        // Add WAF headers
        response.getHeaders().add("X-WAF-Blocked", "true");
        response.getHeaders().add("X-WAF-Reason", wafResult.getReason());
        response.getHeaders().add("X-WAF-Rule-ID", wafResult.getRuleId());
        response.getHeaders().add("X-Request-ID", requestId);

        // Create response body
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now());
        errorResponse.put("status", response.getStatusCode().value());
        errorResponse.put("error", "Request Blocked");
        errorResponse.put("message", "Your request has been blocked by the Web Application Firewall");
        errorResponse.put("reason", wafResult.getReason());
        errorResponse.put("requestId", requestId);

        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(toJson(errorResponse).getBytes())));
    }

    /**
     * Converts error response to JSON (simplified)
     */
    private String toJson(Map<String, Object> response) {
        // In production, use proper JSON serializer
        StringBuilder json = new StringBuilder();
        json.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}