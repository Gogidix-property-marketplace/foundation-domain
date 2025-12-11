package com.gogidix.ai.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Gateway Service Controller
 *
 * Provides health check and routing information for the AI Gateway
 */
@RestController
@RequestMapping("/api")
public class GatewayController {

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AI Gateway Service");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    /**
     * Service information endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "AI Gateway Service");
        response.put("description", "Central gateway for all AI services");
        response.put("version", "1.0.0");
        response.put("endpoints", new String[]{
            "/api/health",
            "/api/info",
            "/api/routes"
        });
        return ResponseEntity.ok(response);
    }

    /**
     * Available routes endpoint
     */
    @GetMapping("/routes")
    public ResponseEntity<Map<String, String>> routes() {
        Map<String, String> response = new HashMap<>();
        response.put("/api/nlp", "Natural Language Processing Service");
        response.put("/api/vision", "Computer Vision Service");
        response.put("/api/predictive", "Predictive Analytics Service");
        response.put("/api/recommendation", "Recommendation Engine Service");
        response.put("/api/chatbot", "Chatbot Service");
        return ResponseEntity.ok(response);
    }
}