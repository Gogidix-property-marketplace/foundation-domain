package com.gogidix.ai.speech.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Request and response logging filter.
 * Logs HTTP requests and responses for debugging and auditing.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();

        try {
            logRequest(requestWrapper, requestId);
            filterChain.doFilter(requestWrapper, responseWrapper);
            logResponse(responseWrapper, requestId, startTime);
        } finally {
            // Copy the response content back to the original response
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Logs the HTTP request details.
     *
     * @param request the request
     * @param requestId the request ID
     */
    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        if (!shouldLog(request)) {
            return;
        }

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("method", request.getMethod());
        logData.put("uri", request.getRequestURI());
        logData.put("queryString", request.getQueryString());
        logData.put("headers", getRequestHeaders(request));
        logData.put("remoteAddr", request.getRemoteAddr());
        logData.put("userAgent", request.getHeader("User-Agent"));

        // Log request body for POST/PUT requests (excluding file uploads)
        if (shouldLogBody(request)) {
            String body = getContent(request.getContentAsByteArray());
            logData.put("body", body);
        }

        log.info("HTTP Request: {}", logData);
    }

    /**
     * Logs the HTTP response details.
     *
     * @param response the response
     * @param requestId the request ID
     * @param startTime the start time
     */
    private void logResponse(ContentCachingResponseWrapper response, String requestId, long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> logData = new HashMap<>();
        logData.put("requestId", requestId);
        logData.put("status", response.getStatus());
        logData.put("contentType", response.getContentType());
        logData.put("duration", duration);

        // Log response body for successful responses (excluding binary content)
        if (shouldLogResponseBody(response)) {
            String body = getContent(response.getContentAsByteArray());
            logData.put("body", body);
        }

        log.info("HTTP Response: {}", logData);
    }

    /**
     * Gets request headers as a map.
     *
     * @param request the request
     * @return headers map
     */
    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }

        return headers;
    }

    /**
     * Converts byte array to string.
     *
     * @param content the byte array
     * @return string content
     */
    private String getContent(byte[] content) {
        if (content.length == 0) {
            return "";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    /**
     * Determines if the request should be logged.
     *
     * @param request the request
     * @return true if should log
     */
    private boolean shouldLog(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Skip health checks and actuator endpoints
        return !uri.contains("/actuator/") && !uri.contains("/health");
    }

    /**
     * Determines if the request body should be logged.
     *
     * @param request the request
     * @return true if should log body
     */
    private boolean shouldLogBody(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();

        return ("POST".equals(method) || "PUT".equals(method)) &&
               contentType != null &&
               !contentType.startsWith("multipart/form-data") &&
               !contentType.contains("image") &&
               !contentType.contains("video");
    }

    /**
     * Determines if the response body should be logged.
     *
     * @param response the response
     * @return true if should log body
     */
    private boolean shouldLogResponseBody(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        int status = response.getStatus();

        return status < 400 &&
               contentType != null &&
               contentType.contains("application/json");
    }

    /**
     * Generates a unique request ID.
     *
     * @return request ID
     */
    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}