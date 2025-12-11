package com.gogidix.platform.api.client;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

/**
 * Mock Platform API Client for AI Integration Service
 * Provides simplified interface to platform services
 */
@Component
public class PlatformApiClient {

    public Map<String, Object> callService(String serviceName, Map<String, Object> request) {
        // Mock implementation
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("service", serviceName);
        response.put("timestamp", System.currentTimeMillis());
        response.put("data", "Mock response from " + serviceName);

        return response;
    }

    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("services", 40);
        health.put("healthy", 40);
        health.put("timestamp", System.currentTimeMillis());

        return health;
    }
}