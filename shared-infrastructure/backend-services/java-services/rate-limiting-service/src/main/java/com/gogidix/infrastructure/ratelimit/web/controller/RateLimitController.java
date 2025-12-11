package com.gogidix.infrastructure.ratelimit.web.controller;

import com.gogidix.infrastructure.ratelimit.application.service.RateLimitService;
import com.gogidix.infrastructure.ratelimit.domain.dto.RateLimitCheckRequest;
import com.gogidix.infrastructure.ratelimit.domain.dto.RateLimitCheckResponse;
import com.gogidix.infrastructure.ratelimit.domain.dto.RateLimitPolicyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Rate Limiting Service.
 *
 * Provides endpoints for:
 * - Rate limit checking
 * - Policy management
 * - Usage statistics
 * - Metrics and monitoring
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rate-limit")
@RequiredArgsConstructor
@Tag(name = "Rate Limiting", description = "Enterprise-grade rate limiting API")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @Operation(
            summary = "Check rate limit",
            description = "Check if a request should be allowed based on rate limiting policies. Returns detailed information about remaining quota and reset time."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rate limit check completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/check")
    public ResponseEntity<RateLimitCheckResponse> checkRateLimit(
            @Valid @RequestBody RateLimitCheckRequest request,
            HttpServletRequest httpRequest) {

        // Enhance request with additional info from HTTP request
        if (request.getIpAddress() == null) {
            request.setIpAddress(getClientIpAddress(httpRequest));
        }
        if (request.getUserAgent() == null) {
            request.setUserAgent(httpRequest.getHeader(HttpHeaders.USER_AGENT));
        }

        RateLimitCheckResponse response = rateLimitService.checkRateLimit(request);

        // Add rate limit headers to response
        HttpHeaders headers = new HttpHeaders();
        if (response.getHeaders() != null) {
            response.getHeaders().forEach(headers::add);
        }

        if (!response.getAllowed()) {
            headers.add("Retry-After", String.valueOf(response.getRetryAfterSeconds() != null ?
                    response.getRetryAfterSeconds() : response.getResetTimeSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(response);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

    @Operation(
            summary = "Create or update rate limit policy",
            description = "Create a new rate limit policy or update an existing one. Supports multiple algorithms and configurations."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:write')")
    @PostMapping("/policies")
    public ResponseEntity<RateLimitPolicyDto> createPolicy(
            @Valid @RequestBody RateLimitPolicyDto policyDto) {

        RateLimitPolicyDto created = rateLimitService.createOrUpdatePolicy(policyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Update rate limit policy",
            description = "Update an existing rate limit policy by ID."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:write')")
    @PutMapping("/policies/{id}")
    public ResponseEntity<RateLimitPolicyDto> updatePolicy(
            @PathVariable String id,
            @Valid @RequestBody RateLimitPolicyDto policyDto) {

        policyDto.setId(id);
        RateLimitPolicyDto updated = rateLimitService.createOrUpdatePolicy(policyDto);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Get rate limit policy",
            description = "Retrieve a specific rate limit policy by ID."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:read')")
    @GetMapping("/policies/{id}")
    public ResponseEntity<RateLimitPolicyDto> getPolicy(@PathVariable String id) {
        RateLimitPolicyDto policy = rateLimitService.getPolicy(id);
        return ResponseEntity.ok(policy);
    }

    @Operation(
            summary = "List policies by tenant",
            description = "Get all rate limit policies for a specific tenant with pagination."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:read')")
    @GetMapping("/policies")
    public ResponseEntity<List<RateLimitPolicyDto>> getPoliciesByTenant(
            @Parameter(description = "Tenant ID") @RequestParam String tenantId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        List<RateLimitPolicyDto> policies = rateLimitService.getPoliciesByTenant(tenantId, page, size);
        return ResponseEntity.ok(policies);
    }

    @Operation(
            summary = "Delete rate limit policy",
            description = "Delete a rate limit policy by ID. Requires tenant authorization."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:delete')")
    @DeleteMapping("/policies/{id}")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable String id,
            @Parameter(description = "Tenant ID for authorization") @RequestParam String tenantId) {

        rateLimitService.deletePolicy(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get usage statistics",
            description = "Retrieve current usage statistics for a specific client and endpoint."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:read')")
    @GetMapping("/usage/{clientId}")
    public ResponseEntity<Object> getUsageStats(
            @PathVariable String clientId,
            @Parameter(description = "Endpoint (optional)") @RequestParam(required = false) String endpoint) {

        Object stats = rateLimitService.getUsageStats(clientId, endpoint);
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Reset counters",
            description = "Reset rate limit counters for a client. Requires authorization."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:reset')")
    @PostMapping("/reset/{clientId}")
    public ResponseEntity<Void> resetCounters(
            @PathVariable String clientId,
            @Parameter(description = "Endpoint (optional)") @RequestParam(required = false) String endpoint,
            @Parameter(description = "Tenant ID for authorization") @RequestParam String tenantId) {

        rateLimitService.resetCounters(clientId, endpoint, tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get service metrics",
            description = "Retrieve real-time metrics and performance statistics for the rate limiting service."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:metrics')")
    @GetMapping("/metrics")
    public ResponseEntity<Object> getMetrics() {
        Object metrics = rateLimitService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    @Operation(
            summary = "Bulk import policies",
            description = "Import multiple rate limit policies at once. Returns import results with success/failure counts."
    )
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('rate-limit:bulk')")
    @PostMapping("/policies/bulk")
    public ResponseEntity<Object> bulkImportPolicies(
            @Parameter(description = "List of policies to import") @RequestBody List<RateLimitPolicyDto> policies,
            @Parameter(description = "Tenant ID") @RequestParam String tenantId) {

        Object result = rateLimitService.bulkImportPolicies(policies, tenantId);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Validate API key",
            description = "Validate an API key and return the associated rate limit policy if valid."
    )
    @GetMapping("/validate/{apiKey}")
    public ResponseEntity<RateLimitPolicyDto> validateApiKey(@PathVariable String apiKey) {
        RateLimitPolicyDto policy = rateLimitService.validateAndGetPolicy(apiKey);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    @Operation(
            summary = "Health check",
            description = "Health check endpoint for the rate limiting service."
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "rate-limiting-service",
                "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(health);
    }

    /**
     * Extract client IP address from HTTP request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}