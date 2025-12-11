package com.gogidix.platform.api.client.keycloak;

import com.gogidix.platform.api.client.keycloak.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Keycloak Identity and Access Management
 *
 * @author Agent A - Platform Architect
 * @version 1.0.0
 */
@FeignClient(
    name = "keycloak-server",
    url = "${gogidix.api.client.keycloak.url:http://localhost:8080}",
    configuration = KeycloakClientConfiguration.class
)
public interface KeycloakClient {

    /**
     * Get admin token
     *
     * @param credentials Token request
     * @return Token response
     */
    @PostMapping("/auth/realms/master/protocol/openid-connect/token")
    TokenResponse getAdminToken(@RequestBody TokenRequest credentials);

    /**
     * Create a new realm
     *
     * @param realm Realm configuration
     * @param authorization Admin token
     * @return Created realm
     */
    @PostMapping("/auth/admin/realms")
    Realm createRealm(
            @RequestBody Realm realm,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get realm information
     *
     * @param realmName Realm name
     * @param authorization Admin token
     * @return Realm details
     */
    @GetMapping("/auth/admin/realms/{realm}")
    Realm getRealm(
            @PathVariable("realm") String realmName,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Create a new user
     *
     * @param realmName Realm name
     * @param user User information
     * @param authorization Admin token
     * @return Created user
     */
    @PostMapping("/auth/admin/realms/{realm}/users")
    User createUser(
            @PathVariable("realm") String realmName,
            @RequestBody User user,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get user by ID
     *
     * @param realmName Realm name
     * @param userId User ID
     * @param authorization Admin token
     * @return User details
     */
    @GetMapping("/auth/admin/realms/{realm}/users/{id}")
    User getUser(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String userId,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Search users
     *
     * @param realmName Realm name
     * @param search Search term
     * @param max Maximum results
     * @param offset Offset for pagination
     * @param authorization Admin token
     * @return List of users
     */
    @GetMapping("/auth/admin/realms/{realm}/users")
    List<User> searchUsers(
            @PathVariable("realm") String realmName,
            @RequestParam("search") String search,
            @RequestParam(value = "max", defaultValue = "10") int max,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Set user password
     *
     * @param realmName Realm name
     * @param userId User ID
     * @param credentials Password credentials
     * @param authorization Admin token
     */
    @PutMapping("/auth/admin/realms/{realm}/users/{id}/reset-password")
    void setUserPassword(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String userId,
            @RequestBody Credentials credentials,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Create a client
     *
     * @param realmName Realm name
     * @param client Client configuration
     * @param authorization Admin token
     * @return Created client
     */
    @PostMapping("/auth/admin/realms/{realm}/clients")
    Client createClient(
            @PathVariable("realm") String realmName,
            @RequestBody Client client,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get client configuration
     *
     * @param realmName Realm name
     * @param clientId Client ID
     * @param authorization Admin token
     * @return Client details
     */
    @GetMapping("/auth/admin/realms/{realm}/clients/{id}")
    Client getClient(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String clientId,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get client secret
     *
     * @param realmName Realm name
     * @param clientId Client ID
     * @param authorization Admin token
     * @return Client secret
     */
    @GetMapping("/auth/admin/realms/{realm}/clients/{id}/client-secret")
    Map<String, String> getClientSecret(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String clientId,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get user roles
     *
     * @param realmName Realm name
     * @param userId User ID
     * @param authorization Admin token
     * @return List of roles
     */
    @GetMapping("/auth/admin/realms/{realm}/users/{id}/role-mappings")
    List<Role> getUserRoles(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String userId,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Assign roles to user
     *
     * @param realmName Realm name
     * @param userId User ID
     * @param roles Roles to assign
     * @param authorization Admin token
     */
    @PostMapping("/auth/admin/realms/{realm}/users/{id}/role-mappings/realm")
    void assignRolesToUser(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String userId,
            @RequestBody List<Role> roles,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get realm roles
     *
     * @param realmName Realm name
     * @param authorization Admin token
     * @return List of roles
     */
    @GetMapping("/auth/admin/realms/{realm}/roles")
    List<Role> getRealmRoles(
            @PathVariable("realm") String realmName,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Create realm role
     *
     * @param realmName Realm name
     * @param role Role to create
     * @param authorization Admin token
     * @return Created role
     */
    @PostMapping("/auth/admin/realms/{realm}/roles")
    Role createRole(
            @PathVariable("realm") String realmName,
            @RequestBody Role role,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get user sessions
     *
     * @param realmName Realm name
     * @param userId User ID
     * @param authorization Admin token
     * @return List of user sessions
     */
    @GetMapping("/auth/admin/realms/{realm}/users/{id}/sessions")
    List<UserSession> getUserSessions(
            @PathVariable("realm") String realmName,
            @PathVariable("id") String userId,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Logout user session
     *
     * @param realmName Realm name
     * @param sessionId Session ID
     * @param authorization Admin token
     */
    @DeleteMapping("/auth/admin/realms/{realm}/sessions/{session}")
    void logoutUser(
            @PathVariable("realm") String realmName,
            @PathVariable("session") String sessionId,
            @RequestHeader("Authorization") String authorization
    );

    /**
     * Get realm statistics
     *
     * @param realmName Realm name
     * @param authorization Admin token
     * @return Realm statistics
     */
    @GetMapping("/auth/admin/realms/{realm}/statistics")
    Map<String, Object> getRealmStatistics(
            @PathVariable("realm") String realmName,
            @RequestHeader("Authorization") String authorization
    );
}