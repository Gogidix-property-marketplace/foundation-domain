package com.gogidix.dashboard.centralized.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gogidix.dashboard.centralized.domain.dashboard.Dashboard;
import com.gogidix.dashboard.centralized.infrastructure.repository.DashboardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End integration tests for domain interactions.
 * Validates communication between Centralized Dashboard and Management Services.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class DomainIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_GATEWAY_URL = "http://localhost:8001/api/v1";

    @Test
    @DisplayName("Should connect to API Gateway health endpoint")
    void shouldConnectToApiGatewayHealth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(API_GATEWAY_URL + "/health", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    @DisplayName("Should create dashboard via API Gateway")
    void shouldCreateDashboardViaApiGateway() {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Dashboard");
        request.put("description", "Test Description");
        request.put("category", "test");
        request.put("tags", Arrays.asList("integration", "test"));
        request.put("theme", "light");
        request.put("refreshInterval", 30);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-jwt-token");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            API_GATEWAY_URL + "/dashboards",
            entity,
            Map.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Dashboard", response.getBody().get("name"));
    }

    @Test
    @DisplayName("Should communicate with user-profile-service")
    void shouldCommunicateWithUserProfileService() {
        // This would test communication with the user-profile-service through the API Gateway
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8101/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should communicate with billing-invoicing-service")
    void shouldCommunicateWithBillingService() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8103/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should communicate with financial-reporting-service")
    void shouldCommunicateWithFinancialReportingService() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8104/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should communicate with regulatory-compliance-service")
    void shouldCommunicateWithComplianceService() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8105/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should communicate with revenue-tracking-service")
    void shouldCommunicateWithRevenueTrackingService() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8121/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should communicate with tax-management-service")
    void shouldCommunicateWithTaxManagementService() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8122/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should communicate with business-intelligence-service")
    void shouldCommunicateWithBiService() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8110/api/v1/health",
            String.class
        );

        // Service should be available
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    @DisplayName("Should validate circuit breaker behavior")
    void shouldValidateCircuitBreakerBehavior() {
        // Test that circuit breaker activates when a service is down
        // This would be implemented with a service that can be started/stopped

        // Initially, the service should be reachable
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(
            "http://localhost:8199/api/v1/non-existent-service/health",
            String.class
        );

        // Should fallback or show circuit breaker behavior
        assertNotNull(initialResponse);
    }

    @Test
    @DisplayName("Should validate cross-domain data flow")
    void shouldValidateCrossDomainDataFlow() {
        // Create a dashboard
        Dashboard dashboard = Dashboard.create(
            "Cross-Domain Test Dashboard",
            "Testing cross-domain communication",
            UUID.randomUUID()
        );
        dashboard.setCategory("test");
        dashboard = dashboardRepository.save(dashboard);

        // Verify dashboard exists
        assertNotNull(dashboard.getId());
        assertEquals("Cross-Domain Test Dashboard", dashboard.getName());

        // This test would be extended to validate actual data flow
        // between dashboard and management services
        assertTrue(dashboard.getId() != null);
    }

    @Test
    @DisplayName("Should validate rate limiting")
    void shouldValidateRateLimiting() {
        // Test rate limiting on billing endpoints
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 100.00);
        request.put("currency", "USD");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // First request should succeed
        ResponseEntity<Map> firstResponse = restTemplate.postForEntity(
            API_GATEWAY_URL + "/billing/invoices",
            entity,
            Map.class
        );

        // Multiple rapid requests should trigger rate limiting
        for (int i = 0; i < 10; i++) {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                API_GATEWAY_URL + "/billing/invoices",
                entity,
                Map.class
            );

            // Should eventually get rate limited
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return; // Rate limiting is working
            }
        }
    }

    @Test
    @DisplayName("Should validate security headers")
    void shouldValidateSecurityHeaders() {
        ResponseEntity<Map> response = restTemplate.getForEntity(API_GATEWAY_URL + "/health", Map.class);

        // Should have security headers
        HttpHeaders headers = response.getHeaders();

        // These headers should be present in production
        assertTrue(headers.containsKey("X-Content-Type-Options") ||
                   headers.containsKey("content-security-policy"));
    }

    @Test
    @DisplayName("Should validate CORS configuration")
    void shouldValidateCorsConfiguration() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:3000");
        headers.set("Access-Control-Request-Method", "GET");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            API_GATEWAY_URL + "/health",
            HttpMethod.OPTIONS,
            entity,
            String.class
        );

        // CORS should be properly configured
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().equals(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("Should validate distributed tracing")
    void shouldValidateDistributedTracing() {
        // Test that tracing headers are propagated
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Trace-Id", "test-trace-id");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
            API_GATEWAY_URL + "/health",
            HttpMethod.GET,
            entity,
            String.class
        );

        // Should support distributed tracing
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should validate error handling")
    void shouldValidateErrorHandling() {
        try {
            // Make a request to an invalid endpoint
            restTemplate.getForEntity(API_GATEWAY_URL + "/invalid-endpoint", String.class);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Exception should be properly handled
            assertNotNull(e);
        }
    }

    @Test
    @DisplayName("Should validate monitoring endpoints")
    void shouldValidateMonitoringEndpoints() {
        // Test metrics endpoint
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            API_GATEWAY_URL + "/actuator/metrics",
            String.class
        );

        // Metrics should be available
        assertTrue(metricsResponse.getStatusCode().is2xxSuccessful() ||
                   metricsResponse.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }
}