package com.gogidix.platform.api.client.configserver;

import com.gogidix.platform.api.client.model.Configuration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Spring Cloud Config Server
 *
 * @author Agent A - Platform Architect
 * @version 1.0.0
 */
@FeignClient(
    name = "config-server",
    url = "${gogidix.api.client.config-server.url:http://localhost:8007}",
    configuration = ConfigServerClientConfiguration.class,
    path = "/config"
)
public interface ConfigServerClient {

    /**
     * Get configuration for a specific application
     *
     * @param application Application name
     * @param profile Profile name
     * @param label Git branch or label
     * @return Configuration map
     */
    @GetMapping("/{application}-{profile}.yml")
    Map<String, Object> getConfiguration(
            @PathVariable("application") String application,
            @PathVariable("profile") String profile,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get configuration properties
     *
     * @param application Application name
     * @param profile Profile name
     * @param label Git branch or label
     * @return Configuration object
     */
    @GetMapping("/{application}/{profile}")
    Configuration getConfigurationProperties(
            @PathVariable("application") String application,
            @PathVariable("profile") String profile,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get environment-specific configuration
     *
     * @param application Application name
     * @param profile Profile name
     * @param label Git branch or label
     * @return Environment configuration
     */
    @GetMapping("/{application}/{profile}/{label}")
    Map<String, Object> getEnvironmentConfiguration(
            @PathVariable("application") String application,
            @PathVariable("profile") String profile,
            @PathVariable("label") String label,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Encrypt a property value
     *
     * @param value Value to encrypt
     * @param authorization Authorization header
     * @return Encrypted value
     */
    @PostMapping("/encrypt")
    String encryptProperty(
            Map<String, String> value,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Decrypt a property value
     *
     * @param value Value to decrypt
     * @param authorization Authorization header
     * @return Decrypted value
     */
    @PostMapping("/decrypt")
    String decryptProperty(
            Map<String, String> value,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Refresh configuration for all services
     *
     * @param authorization Authorization header
     * @return Refresh status
     */
    @PostMapping("/bus-refresh")
    Map<String, Object> refreshAllServices(
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Refresh configuration for specific service
     *
     * @param destination Destination service
     * @param authorization Authorization header
     * @return Refresh status
     */
    @PostMapping("/bus-refresh")
    Map<String, Object> refreshSpecificService(
            @PathVariable("destination") String destination,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get list of available applications
     *
     * @param authorization Authorization header
     * @return List of applications
     */
    @GetMapping("/applications")
    List<String> getApplications(
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get health status of config server
     *
     * @return Health status
     */
    @GetMapping("/actuator/health")
    Map<String, Object> getHealthStatus();
}