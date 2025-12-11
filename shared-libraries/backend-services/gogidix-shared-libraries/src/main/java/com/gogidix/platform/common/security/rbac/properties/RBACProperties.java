package com.gogidix.platform.common.security.rbac.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * 🏗️ GOGIDIX FOUNDATION - RBAC CONFIGURATION PROPERTIES
 *
 * Centralized RBAC configuration for all Gogidix services
 *
 * Features:
 * - Role hierarchy definition
 * - Permission mapping
 * - CORS configuration
 * - JWT claim configuration
 * - Cross-domain role inheritance
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 * @since 2025-12-03
 */
@Component
@ConfigurationProperties(prefix = "gogidix.foundation.security.rbac")
public class RBACProperties {

    private boolean enabled = true;
    private String defaultRole = "USER";
    private RoleHierarchy roles = new RoleHierarchy();
    private CORSConfiguration cors = new CORSConfiguration();
    private JWTConfiguration jwt = new JWTConfiguration();
    private RateLimitingConfiguration rateLimiting = new RateLimitingConfiguration();

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDefaultRole() { return defaultRole; }
    public void setDefaultRole(String defaultRole) { this.defaultRole = defaultRole; }

    public RoleHierarchy getRoles() { return roles; }
    public void setRoles(RoleHierarchy roles) { this.roles = roles; }

    public CORSConfiguration getCors() { return cors; }
    public void setCors(CORSConfiguration cors) { this.cors = cors; }

    public JWTConfiguration getJwt() { return jwt; }
    public void setJwt(JWTConfiguration jwt) { this.jwt = jwt; }

    public RateLimitingConfiguration getRateLimiting() { return rateLimiting; }
    public void setRateLimiting(RateLimitingConfiguration rateLimiting) { this.rateLimiting = rateLimiting; }

    /**
     * 🏢 Role Hierarchy Configuration
     */
    public static class RoleHierarchy {
        private List<String> admin = Arrays.asList("ADMIN", "PLATFORM_ADMIN", "SYSTEM_ADMIN");
        private List<String> management = Arrays.asList("USER_MANAGER", "PROPERTY_MANAGER", "FINANCE_MANAGER", "NOTIFICATION_MANAGER", "REPORT_MANAGER");
        private List<String> operational = Arrays.asList("AGENT", "ANALYST", "ACCOUNTANT", "SUPPORT_AGENT");
        private List<String> business = Arrays.asList("TENANT", "LANDLORD", "PROPERTY_OWNER");
        private List<String> user = Arrays.asList("USER", "CUSTOMER");
        private List<String> service = Arrays.asList("SERVICE", "MICROSERVICE");

        // Role hierarchy mapping (higher to lower)
        private Map<String, List<String>> hierarchy = new HashMap<>();

        public RoleHierarchy() {
            // Initialize role hierarchy
            hierarchy.put("ADMIN", Arrays.asList("PLATFORM_ADMIN", "SYSTEM_ADMIN", "USER_MANAGER", "PROPERTY_MANAGER", "FINANCE_MANAGER", "NOTIFICATION_MANAGER", "REPORT_MANAGER", "AGENT", "ANALYST", "ACCOUNTANT", "SUPPORT_AGENT", "TENANT", "LANDLORD", "PROPERTY_OWNER", "USER", "CUSTOMER"));
            hierarchy.put("PLATFORM_ADMIN", Arrays.asList("SYSTEM_ADMIN", "USER_MANAGER", "PROPERTY_MANAGER", "FINANCE_MANAGER", "NOTIFICATION_MANAGER", "REPORT_MANAGER"));
            hierarchy.put("SYSTEM_ADMIN", Arrays.asList("USER_MANAGER", "PROPERTY_MANAGER", "FINANCE_MANAGER", "NOTIFICATION_MANAGER", "REPORT_MANAGER", "SERVICE", "MICROSERVICE"));
            hierarchy.put("USER_MANAGER", Arrays.asList("USER", "CUSTOMER"));
            hierarchy.put("PROPERTY_MANAGER", Arrays.asList("AGENT", "TENANT", "LANDLORD", "PROPERTY_OWNER"));
            hierarchy.put("FINANCE_MANAGER", Arrays.asList("ACCOUNTANT"));
            hierarchy.put("NOTIFICATION_MANAGER", Arrays.asList("SUPPORT_AGENT"));
            hierarchy.put("REPORT_MANAGER", Arrays.asList("ANALYST"));
        }

        // Getters
        public List<String> getAdmin() { return admin; }
        public void setAdmin(List<String> admin) { this.admin = admin; }

        public List<String> getManagement() { return management; }
        public void setManagement(List<String> management) { this.management = management; }

        public List<String> getOperational() { return operational; }
        public void setOperational(List<String> operational) { this.operational = operational; }

        public List<String> getBusiness() { return business; }
        public void setBusiness(List<String> business) { this.business = business; }

        public List<String> getUser() { return user; }
        public void setUser(List<String> user) { this.user = user; }

        public List<String> getService() { return service; }
        public void setService(List<String> service) { this.service = service; }

        public Map<String, List<String>> getHierarchy() { return hierarchy; }
        public void setHierarchy(Map<String, List<String>> hierarchy) { this.hierarchy = hierarchy; }
    }

    /**
     * 🌐 CORS Configuration
     */
    public static class CORSConfiguration {
        private boolean enabled = true;
        private List<String> allowedOrigins = Arrays.asList("http://localhost:3000", "http://localhost:8080", "https://gogidix.com");
        private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        private List<String> allowedHeaders = Arrays.asList("*");
        private boolean allowCredentials = true;
        private long maxAge = 3600L;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }

        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }

        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }

        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }

        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }

    /**
     * 🔑 JWT Configuration
     */
    public static class JWTConfiguration {
        private String rolesClaim = "roles";
        private String permissionsClaim = "permissions";
        private String domainClaim = "domain";
        private String serviceClaim = "service";
        private String userIdClaim = "sub";
        private String usernameClaim = "preferred_username";
        private String emailClaim = "email";
        private String nameClaim = "name";

        // JWT validation settings
        private boolean validateIssuer = true;
        private boolean validateAudience = true;
        private boolean validateExpiration = true;
        private boolean clockSkew = true;
        private long maxClockSkewSeconds = 30L;

        // Getters and setters
        public String getRolesClaim() { return rolesClaim; }
        public void setRolesClaim(String rolesClaim) { this.rolesClaim = rolesClaim; }

        public String getPermissionsClaim() { return permissionsClaim; }
        public void setPermissionsClaim(String permissionsClaim) { this.permissionsClaim = permissionsClaim; }

        public String getDomainClaim() { return domainClaim; }
        public void setDomainClaim(String domainClaim) { this.domainClaim = domainClaim; }

        public String getServiceClaim() { return serviceClaim; }
        public void setServiceClaim(String serviceClaim) { this.serviceClaim = serviceClaim; }

        public String getUserIdClaim() { return userIdClaim; }
        public void setUserIdClaim(String userIdClaim) { this.userIdClaim = userIdClaim; }

        public String getUsernameClaim() { return usernameClaim; }
        public void setUsernameClaim(String usernameClaim) { this.usernameClaim = usernameClaim; }

        public String getEmailClaim() { return emailClaim; }
        public void setEmailClaim(String emailClaim) { this.emailClaim = emailClaim; }

        public String getNameClaim() { return nameClaim; }
        public void setNameClaim(String nameClaim) { this.nameClaim = nameClaim; }

        public boolean isValidateIssuer() { return validateIssuer; }
        public void setValidateIssuer(boolean validateIssuer) { this.validateIssuer = validateIssuer; }

        public boolean isValidateAudience() { return validateAudience; }
        public void setValidateAudience(boolean validateAudience) { this.validateAudience = validateAudience; }

        public boolean isValidateExpiration() { return validateExpiration; }
        public void setValidateExpiration(boolean validateExpiration) { this.validateExpiration = validateExpiration; }

        public boolean isClockSkew() { return clockSkew; }
        public void setClockSkew(boolean clockSkew) { this.clockSkew = clockSkew; }

        public long getMaxClockSkewSeconds() { return maxClockSkewSeconds; }
        public void setMaxClockSkewSeconds(long maxClockSkewSeconds) { this.maxClockSkewSeconds = maxClockSkewSeconds; }
    }

    /**
     * ⚡ Rate Limiting Configuration
     */
    public static class RateLimitingConfiguration {
        private boolean enabled = true;
        private int defaultLimit = 1000;
        private int windowSeconds = 60;
        private List<String> exemptRoles = Arrays.asList("ADMIN", "PLATFORM_ADMIN", "SYSTEM_ADMIN", "SERVICE");
        private Map<String, Integer> roleLimits = new HashMap<>();

        public RateLimitingConfiguration() {
            // Initialize role-specific limits
            roleLimits.put("ADMIN", Integer.MAX_VALUE);
            roleLimits.put("PLATFORM_ADMIN", Integer.MAX_VALUE);
            roleLimits.put("SYSTEM_ADMIN", Integer.MAX_VALUE);
            roleLimits.put("SERVICE", 10000);
            roleLimits.put("USER_MANAGER", 5000);
            roleLimits.put("PROPERTY_MANAGER", 2000);
            roleLimits.put("FINANCE_MANAGER", 2000);
            roleLimits.put("NOTIFICATION_MANAGER", 5000);
            roleLimits.put("REPORT_MANAGER", 1000);
            roleLimits.put("AGENT", 1000);
            roleLimits.put("ANALYST", 500);
            roleLimits.put("ACCOUNTANT", 500);
            roleLimits.put("SUPPORT_AGENT", 2000);
            roleLimits.put("TENANT", 500);
            roleLimits.put("LANDLORD", 500);
            roleLimits.put("PROPERTY_OWNER", 500);
            roleLimits.put("USER", 100);
            roleLimits.put("CUSTOMER", 50);
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getDefaultLimit() { return defaultLimit; }
        public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }

        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

        public List<String> getExemptRoles() { return exemptRoles; }
        public void setExemptRoles(List<String> exemptRoles) { this.exemptRoles = exemptRoles; }

        public Map<String, Integer> getRoleLimits() { return roleLimits; }
        public void setRoleLimits(Map<String, Integer> roleLimits) { this.roleLimits = roleLimits; }
    }
}