package com.gogidix.infrastructure.gateway.domain.versioning;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Enterprise API Version Manager.
 *
 * Manages API versions with:
 * - Semantic versioning (semver)
 * - Version lifecycle management
 * - Deprecation policies
 * - Breaking change detection
 * - Automated version routing
 * - Version compatibility matrix
 * - Sunset policy enforcement
 *
 * @author Gogidix Infrastructure Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ApiVersionManager {

    // In-memory storage for version configurations
    private final Map<String, ApiVersion> versions = new ConcurrentHashMap<>();
    private final Map<String, List<ApiRoute>> versionRoutes = new ConcurrentHashMap<>();
    private final Map<String, VersionCompatibility> compatibilityMatrix = new ConcurrentHashMap<>();

    // Version pattern for validation
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+(\\.\\d+)*(-[a-zA-Z0-9]+)?$");

    /**
     * Registers a new API version
     */
    public void registerVersion(ApiVersion version) {
        validateVersion(version);

        versions.put(version.getVersion(), version);
        versionRoutes.put(version.getVersion(), new ArrayList<>());

        log.info("Registered API version: {} - {}", version.getVersion(), version.getDescription());
    }

    /**
     * Registers an API route for a specific version
     */
    public void registerRoute(String version, ApiRoute route) {
        if (!versions.containsKey(version)) {
            throw new IllegalArgumentException("Version " + version + " is not registered");
        }

        validateRoute(route, version);

        List<ApiRoute> routes = versionRoutes.computeIfAbsent(version, k -> new ArrayList<>());
        routes.add(route);

        // Sort routes by priority
        routes.sort(Comparator.comparingInt(ApiRoute::getPriority).reversed());

        log.debug("Registered route for version {}: {} {}", version, route.getHttpMethod(), route.getPath());
    }

    /**
     * Gets the best matching version for a client
     */
    public String getBestVersion(String requestedVersion, String clientType) {
        // If no version requested, return latest stable
        if (requestedVersion == null || requestedVersion.isEmpty()) {
            return getLatestStableVersion();
        }

        // Normalize requested version
        String normalizedVersion = normalizeVersion(requestedVersion);

        // Check if exact version exists
        if (versions.containsKey(normalizedVersion)) {
            ApiVersion version = versions.get(normalizedVersion);

            // Check if version is accessible
            if (isVersionAccessible(version, clientType)) {
                return normalizedVersion;
            }
        }

        // Find closest compatible version
        return findCompatibleVersion(normalizedVersion, clientType);
    }

    /**
     * Gets all routes for a specific version
     */
    public List<ApiRoute> getRoutesForVersion(String version) {
        return versionRoutes.getOrDefault(version, Collections.emptyList());
    }

    /**
     * Gets version information
     */
    public Optional<ApiVersion> getVersion(String version) {
        return Optional.ofNullable(versions.get(normalizeVersion(version)));
    }

    /**
     * Gets all registered versions
     */
    public List<ApiVersion> getAllVersions() {
        return new ArrayList<>(versions.values());
    }

    /**
     * Gets active versions
     */
    public List<ApiVersion> getActiveVersions() {
        return versions.values().stream()
                .filter(v -> v.getStatus() == VersionStatus.ACTIVE || v.getStatus() == VersionStatus.DEPRECATED)
                .sorted(Comparator.comparing(ApiVersion::getReleaseDate).reversed())
                .toList();
    }

    /**
     * Gets deprecated versions
     */
    public List<ApiVersion> getDeprecatedVersions() {
        return versions.values().stream()
                .filter(v -> v.getStatus() == VersionStatus.DEPRECATED)
                .sorted(Comparator.comparing(ApiVersion::getDeprecationDate))
                .toList();
    }

    /**
     * Gets latest stable version
     */
    public String getLatestStableVersion() {
        return versions.values().stream()
                .filter(v -> v.getStatus() == VersionStatus.ACTIVE && v.isStable())
                .max(Comparator.comparing(ApiVersion::getReleaseDate))
                .map(ApiVersion::getVersion)
                .orElse("v1.0.0");
    }

    /**
     * Marks a version as deprecated
     */
    public void deprecateVersion(String version, String deprecationReason, LocalDateTime sunsetDate) {
        ApiVersion apiVersion = versions.get(normalizeVersion(version));
        if (apiVersion == null) {
            throw new IllegalArgumentException("Version " + version + " not found");
        }

        apiVersion.setStatus(VersionStatus.DEPRECATED);
        apiVersion.setDeprecationReason(deprecationReason);
        apiVersion.setDeprecationDate(LocalDateTime.now());
        apiVersion.setSunsetDate(sunsetDate);

        log.warn("Deprecated API version: {} - Sunset date: {}", version, sunsetDate);

        // Send deprecation notifications
        sendDeprecationNotifications(apiVersion);
    }

    /**
     * Retires a version
     */
    public void retireVersion(String version) {
        ApiVersion apiVersion = versions.get(normalizeVersion(version));
        if (apiVersion == null) {
            throw new IllegalArgumentException("Version " + version + " not found");
        }

        apiVersion.setStatus(VersionStatus.RETIRED);
        apiVersion.setRetirementDate(LocalDateTime.now());

        log.info("Retired API version: {}", version);

        // Remove routes for retired version
        versionRoutes.remove(version);
    }

    /**
     * Checks if a version is accessible by a client type
     */
    public boolean isVersionAccessible(String version, String clientType) {
        ApiVersion apiVersion = versions.get(normalizeVersion(version));
        if (apiVersion == null) {
            return false;
        }

        return isVersionAccessible(apiVersion, clientType);
    }

    /**
     * Gets version compatibility between two versions
     */
    public VersionCompatibility getCompatibility(String fromVersion, String toVersion) {
        String key = fromVersion + "->" + toVersion;
        return compatibilityMatrix.getOrDefault(key, VersionCompatibility.UNKNOWN);
    }

    /**
     * Sets version compatibility
     */
    public void setCompatibility(String fromVersion, String toVersion, VersionCompatibility compatibility) {
        String key = fromVersion + "->" + toVersion;
        compatibilityMatrix.put(key, compatibility);
        log.debug("Set compatibility: {} = {}", key, compatibility);
    }

    /**
     * Validates breaking changes
     */
    public BreakingChangeValidationResult validateBreakingChanges(String fromVersion, String toVersion, List<BreakingChange> changes) {
        BreakingChangeValidationResult result = new BreakingChangeValidationResult();

        // Check if changes are breaking
        boolean hasBreakingChanges = changes.stream()
                .anyMatch(change -> change.isBreaking());

        if (hasBreakingChanges) {
            // Check if compatibility allows breaking changes
            VersionCompatibility compatibility = getCompatibility(fromVersion, toVersion);
            if (compatibility == VersionCompatibility.COMPATIBLE) {
                result.setValid(false);
                result.addError("Breaking changes detected but versions marked as compatible");
            } else {
                result.setValid(true);
                result.addWarning("Breaking changes detected - compatibility matrix updated");
            }
        } else {
            result.setValid(true);
        }

        // Check deprecation policy
        ApiVersion fromApiVersion = versions.get(normalizeVersion(fromVersion));
        if (fromApiVersion != null && fromApiVersion.getStatus() == VersionStatus.DEPRECATED) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(fromApiVersion.getSunsetDate())) {
                result.addError("Source version " + fromVersion + " has passed its sunset date");
                result.setValid(false);
            } else {
                result.addWarning("Source version " + fromVersion + " is deprecated and will sunset on " + fromApiVersion.getSunsetDate());
            }
        }

        return result;
    }

    // Private helper methods

    private void validateVersion(ApiVersion version) {
        if (!VERSION_PATTERN.matcher(version.getVersion()).matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version.getVersion());
        }

        if (version.getReleaseDate() == null) {
            throw new IllegalArgumentException("Release date is required");
        }
    }

    private void validateRoute(ApiRoute route, String version) {
        // Validate route path includes version
        if (!route.getPath().startsWith("/" + version + "/")) {
            throw new IllegalArgumentException("Route path must start with /" + version + "/");
        }
    }

    private String normalizeVersion(String version) {
        if (version.startsWith("v")) {
            return version;
        }
        return "v" + version;
    }

    private boolean isVersionAccessible(ApiVersion version, String clientType) {
        // Check status
        if (version.getStatus() == VersionStatus.DRAFT || version.getStatus() == VersionStatus.RETIRED) {
            return false;
        }

        // Check client access
        if (version.getAllowedClients() != null && !version.getAllowedClients().isEmpty()) {
            return version.getAllowedClients().contains(clientType) || version.getAllowedClients().contains("*");
        }

        if (version.getBlockedClients() != null) {
            return !version.getBlockedClients().contains(clientType);
        }

        return true;
    }

    private String findCompatibleVersion(String requestedVersion, String clientType) {
        // Parse requested version to extract major/minor
        String[] requestedParts = requestedVersion.substring(1).split("\\.");
        int requestedMajor = Integer.parseInt(requestedParts[0]);
        int requestedMinor = requestedParts.length > 1 ? Integer.parseInt(requestedParts[1]) : 0;

        // Find versions with same major version
        List<ApiVersion> compatibleVersions = versions.values().stream()
                .filter(v -> isVersionAccessible(v, clientType))
                .filter(v -> {
                    String[] parts = v.getVersion().substring(1).split("\\.");
                    int major = Integer.parseInt(parts[0]);
                    return major == requestedMajor;
                })
                .sorted((v1, v2) -> {
                    // Prefer stable versions
                    boolean v1Stable = v1.isStable();
                    boolean v2Stable = v2.isStable();
                    if (v1Stable != v2Stable) {
                        return v2Stable ? -1 : 1;
                    }
                    // Prefer higher minor version
                    String[] parts1 = v1.getVersion().substring(1).split("\\.");
                    String[] parts2 = v2.getVersion().substring(1).split("\\.");
                    int minor1 = parts1.length > 1 ? Integer.parseInt(parts1[1]) : 0;
                    int minor2 = parts2.length > 1 ? Integer.parseInt(parts2[1]) : 0;
                    return Integer.compare(minor2, minor1);
                })
                .toList();

        return compatibleVersions.isEmpty() ? getLatestStableVersion() : compatibleVersions.get(0).getVersion();
    }

    private void sendDeprecationNotifications(ApiVersion version) {
        // Implementation would send notifications to clients using registered channels
        log.info("Sending deprecation notifications for version {} to sunset on {}",
                version.getVersion(), version.getSunsetDate());
    }

    // Data classes

    @Data
    public static class ApiVersion {
        private String version;
        private String description;
        private VersionStatus status;
        private boolean stable;
        private LocalDateTime releaseDate;
        private LocalDateTime deprecationDate;
        private LocalDateTime sunsetDate;
        private LocalDateTime retirementDate;
        private String deprecationReason;
        private List<String> allowedClients;
        private List<String> blockedClients;
        private Map<String, Object> metadata;
    }

    @Data
    public static class ApiRoute {
        private String id;
        private String httpMethod;
        private String path;
        private String handler;
        private int priority;
        private Map<String, Object> metadata;
    }

    public enum VersionStatus {
        DRAFT,
        ACTIVE,
        DEPRECATED,
        RETIRED
    }

    public enum VersionCompatibility {
        COMPATIBLE,
        BACKWARD_COMPATIBLE,
        FORWARD_COMPATIBLE,
        INCOMPATIBLE,
        UNKNOWN
    }

    @Data
    public static class BreakingChange {
        private String type;
        private String description;
        private boolean breaking;
        private String component;
    }

    @Data
    public static class BreakingChangeValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }
    }
}