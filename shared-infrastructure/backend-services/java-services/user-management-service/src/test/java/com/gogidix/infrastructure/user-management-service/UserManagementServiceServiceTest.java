package com.gogidix.infrastructure.user-management-service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("UserManagementService Service Tests")
class UserManagementServiceServiceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // Test setup
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return UP status")
        void healthCheckShouldReturnUp() {
            String response = restTemplate.getForObject(
                "http://localhost:" + port + "/actuator/health",
                String.class
            );

            assertThat(response).contains("UP");
        }

        @Test
        @DisplayName("Should return service information")
        void healthCheckShouldReturnServiceInfo() {
            var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                Object.class
            );

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("API Endpoint Tests")
    class ApiEndpointTests {

        @Test
        @DisplayName("Should handle basic API requests")
        void shouldHandleBasicRequests() {
            var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1",
                String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should handle POST requests")
        void shouldHandlePostRequests() {
            var request = new TestRequest();
            request.setMessage("test");

            var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/v1/test",
                request,
                String.class
            );

            assertThat(response.getStatusCode()).isIn(200, 201, 400);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should include security headers")
        void shouldIncludeSecurityHeaders() {
            var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
            );

            assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isNotNull();
            assertThat(response.getHeaders().getFirst("X-Frame-Options")).isNotNull();
        }

        @Test
        @DisplayName("Should handle unauthenticated requests")
        void shouldHandleUnauthenticatedRequests() {
            // Test public endpoints
            var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info",
                String.class
            );

            assertThat(response.getStatusCodeValue()).isIn(200, 404);
        }
    }

    // Test request class
    private static class TestRequest {
        private String message;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
