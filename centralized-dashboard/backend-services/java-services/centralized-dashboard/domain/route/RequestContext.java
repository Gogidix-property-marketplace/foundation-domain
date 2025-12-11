package com.gogidix.infrastructure.gateway.domain.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Context object containing request information for routing decisions.
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {

    /**
     * Unique request ID for tracing
     */
    private String requestId;

    /**
     * Correlation ID for distributed tracing
     */
    private String correlationId;

    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    private String httpMethod;

    /**
     * Request path and query string
     */
    private String path;

    /**
     * Request path without query string
     */
    private String uri;

    /**
     * HTTP headers
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * Query parameters
     */
    @Builder.Default
    private Map<String, String> queryParams = new HashMap<>();

    /**
     * Request body as string
     */
    private String body;

    /**
     * Client IP address
     */
    private String clientIp;

    /**
     * User agent string
     */
    private String userAgent;

    /**
     * User ID if authenticated
     */
    private String userId;

    /**
     * User roles for authorization
     */
    @Builder.Default
    private List<String> userRoles = List.of();

    /**
     * User segment (VIP, PREMIUM, FREE, etc.)
     */
    private String userSegment;

    /**
     * User's country code (ISO 3166-1 alpha-2)
     */
    private String country;

    /**
     * User's region/state
     */
    private String region;

    /**
     * Request timestamp
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Session ID if available
     */
    private String sessionId;

    /**
     * API key if present
     */
    private String apiKey;

    /**
     * Authentication token (JWT, OAuth, etc.)
     */
    private String authToken;

    /**
     * Content type of the request
     */
    private String contentType;

    /**
     * Content length
     */
    private Long contentLength;

    /**
     * Whether this is a secure (HTTPS) request
     */
    private boolean secure;

    /**
     * Protocol version (HTTP/1.1, HTTP/2, etc.)
     */
    private String protocol;

    /**
     * Host name
     */
    private String host;

    /**
     * Original request URI before any transformations
     */
    private String originalUri;

    /**
     * Additional metadata about the request
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Creates a new request context with required fields
     */
    public static RequestContext create(String httpMethod, String uri, String clientIp) {
        return RequestContext.builder()
                .requestId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .httpMethod(httpMethod)
                .uri(uri)
                .path(uri)
                .clientIp(clientIp)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Gets a header value with default
     */
    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * Gets a header value with default
     */
    public String getHeader(String name, String defaultValue) {
        return headers.getOrDefault(name, defaultValue);
    }

    /**
     * Checks if a header exists
     */
    public boolean hasHeader(String name) {
        return headers.containsKey(name);
    }

    /**
     * Adds a header
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Gets a query parameter value
     */
    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    /**
     * Gets a query parameter value with default
     */
    public String getQueryParam(String name, String defaultValue) {
        return queryParams.getOrDefault(name, defaultValue);
    }

    /**
     * Checks if a query parameter exists
     */
    public boolean hasQueryParam(String name) {
        return queryParams.containsKey(name);
    }

    /**
     * Adds a query parameter
     */
    public void addQueryParam(String name, String value) {
        queryParams.put(name, value);
    }

    /**
     * Checks if user has any of the specified roles
     */
    public boolean hasAnyRole(List<String> roles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(userRoles::contains);
    }

    /**
     * Checks if user has the specified role
     */
    public boolean hasRole(String role) {
        return userRoles != null && userRoles.contains(role);
    }

    /**
     * Checks if user is authenticated
     */
    public boolean isAuthenticated() {
        return userId != null || authToken != null;
    }

    /**
     * Gets content type without charset
     */
    public String getBaseContentType() {
        if (contentType == null) {
            return null;
        }
        int semiColonIndex = contentType.indexOf(';');
        if (semiColonIndex > 0) {
            return contentType.substring(0, semiColonIndex).trim();
        }
        return contentType;
    }

    /**
     * Checks if this is a JSON request
     */
    public boolean isJsonRequest() {
        String baseContentType = getBaseContentType();
        return "application/json".equals(baseContentType);
    }

    /**
     * Checks if this is an XML request
     */
    public boolean isXmlRequest() {
        String baseContentType = getBaseContentType();
        return "application/xml".equals(baseContentType) || "text/xml".equals(baseContentType);
    }

    /**
     * Checks if this is a form request
     */
    public boolean isFormRequest() {
        String baseContentType = getBaseContentType();
        return "application/x-www-form-urlencoded".equals(baseContentType) ||
               "multipart/form-data".equals(baseContentType);
    }

    /**
     * Adds metadata
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Gets metadata
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Creates a copy of this context
     */
    public RequestContext copy() {
        return RequestContext.builder()
                .requestId(this.requestId)
                .correlationId(this.correlationId)
                .httpMethod(this.httpMethod)
                .path(this.path)
                .uri(this.uri)
                .headers(new HashMap<>(this.headers))
                .queryParams(new HashMap<>(this.queryParams))
                .body(this.body)
                .clientIp(this.clientIp)
                .userAgent(this.userAgent)
                .userId(this.userId)
                .userRoles(List.copyOf(this.userRoles))
                .userSegment(this.userSegment)
                .country(this.country)
                .region(this.region)
                .timestamp(this.timestamp)
                .sessionId(this.sessionId)
                .apiKey(this.apiKey)
                .authToken(this.authToken)
                .contentType(this.contentType)
                .contentLength(this.contentLength)
                .secure(this.secure)
                .protocol(this.protocol)
                .host(this.host)
                .originalUri(this.originalUri)
                .metadata(new HashMap<>(this.metadata))
                .build();
    }

    /**
     * Creates a child context with same correlation ID but new request ID
     */
    public RequestContext createChild() {
        return RequestContext.builder()
                .requestId(UUID.randomUUID().toString())
                .correlationId(this.correlationId)
                .httpMethod(this.httpMethod)
                .path(this.path)
                .uri(this.uri)
                .headers(new HashMap<>(this.headers))
                .queryParams(new HashMap<>(this.queryParams))
                .body(this.body)
                .clientIp(this.clientIp)
                .userAgent(this.userAgent)
                .userId(this.userId)
                .userRoles(List.copyOf(this.userRoles))
                .userSegment(this.userSegment)
                .country(this.country)
                .region(this.region)
                .timestamp(Instant.now())
                .sessionId(this.sessionId)
                .apiKey(this.apiKey)
                .authToken(this.authToken)
                .contentType(this.contentType)
                .contentLength(this.contentLength)
                .secure(this.secure)
                .protocol(this.protocol)
                .host(this.host)
                .originalUri(this.originalUri)
                .metadata(new HashMap<>(this.metadata))
                .build();
    }

    @Override
    public String toString() {
        return "RequestContext{" +
                "requestId='" + requestId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", uri='" + uri + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}