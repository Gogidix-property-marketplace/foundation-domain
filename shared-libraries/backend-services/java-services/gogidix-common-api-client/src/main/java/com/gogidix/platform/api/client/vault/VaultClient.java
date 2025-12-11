package com.gogidix.platform.api.client.vault;

import com.gogidix.platform.api.client.vault.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for HashiCorp Vault Secret Management
 *
 * @author Agent A - Platform Architect
 * @version 1.0.0
 */
@FeignClient(
    name = "vault-server",
    url = "${gogidix.api.client.vault.url:http://localhost:8200}",
    configuration = VaultClientConfiguration.class
)
public interface VaultClient {

    /**
     * Initialize vault
     *
     * @param initRequest Initialization request
     * @param token Vault token
     * @return Initialization response
     */
    @PutMapping("/v1/sys/init")
    InitResponse initializeVault(
            @RequestBody InitRequest initRequest,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Unseal vault
     *
     * @param unsealRequest Unseal request
     * @return Unseal response
     */
    @PutMapping("/v1/sys/unseal")
    UnsealResponse unsealVault(@RequestBody UnsealRequest unsealRequest);

    /**
     * Get vault status
     *
     * @return Vault status
     */
    @GetMapping("/v1/sys/seal-status")
    SealStatus getSealStatus();

    /**
     * Get health status
     *
     * @return Health status
     */
    @GetMapping("/v1/sys/health")
    HealthStatus getHealthStatus();

    /**
     * Enable secret engine
     *
     * @param engine Secret engine configuration
     * @param token Vault token
     * @return Response
     */
    @PostMapping("/v1/sys/mounts/{path}")
    Map<String, Object> enableSecretEngine(
            @PathVariable("path") String path,
            @RequestBody SecretEngine engine,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Read secret
     *
     * @param path Secret path
     * @param token Vault token
     * @return Secret data
     */
    @GetMapping("/v1/secret/data/{path}")
    SecretData readSecret(
            @PathVariable("path") String path,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Write secret
     *
     * @param path Secret path
     * @param secret Secret data
     * @param token Vault token
     * @return Response
     */
    @PostMapping("/v1/secret/data/{path}")
    Map<String, Object> writeSecret(
            @PathVariable("path") String path,
            @RequestBody WriteSecretRequest secret,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Delete secret
     *
     * @param path Secret path
     * @param token Vault token
     * @return Response
     */
    @DeleteMapping("/v1/secret/data/{path}")
    Map<String, Object> deleteSecret(
            @PathVariable("path") String path,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * List secrets
     *
     * @param path Secret path
     * @param token Vault token
     * @return List of secrets
     */
    @GetMapping("/v1/secret/metadata/{path}")
    Map<String, Object> listSecrets(
            @PathVariable("path") String path,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Create/update policy
     *
     * @param name Policy name
     * @param policy Policy content
     * @param token Vault token
     * @return Response
     */
    @PutMapping("/v1/sys/policies/acl/{name}")
    Map<String, Object> createPolicy(
            @PathVariable("name") String name,
            @RequestBody Policy policy,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Get policy
     *
     * @param name Policy name
     * @param token Vault token
     * @return Policy
     */
    @GetMapping("/v1/sys/policies/acl/{name}")
    Policy getPolicy(
            @PathVariable("name") String name,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Create token
     *
     * @param tokenRequest Token creation request
     * @param token Vault token
     * @return Token response
     */
    @PostMapping("/v1/auth/token/create")
    TokenResponse createToken(
            @RequestBody TokenRequest tokenRequest,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Lookup token
     *
     * @param tokenToLookup Token to lookup
     * @param token Vault token
     * @return Token information
     */
    @PostMapping("/v1/auth/token/lookup")
    Map<String, Object> lookupToken(
            @RequestBody Map<String, String> tokenToLookup,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Revoke token
     *
     * @param tokenToRevoke Token to revoke
     * @param token Vault token
     * @return Response
     */
    @PostMapping("/v1/auth/token/revoke")
    Map<String, Object> revokeToken(
            @RequestBody Map<String, String> tokenToRevoke,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Enable auth method
     *
     * @param path Auth method path
     * @param authMethod Auth method configuration
     * @param token Vault token
     * @return Response
     */
    @PostMapping("/v1/sys/auth/{path}")
    Map<String, Object> enableAuthMethod(
            @PathVariable("path") String path,
            @RequestBody AuthMethod authMethod,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Get audit devices
     *
     * @param token Vault token
     * @return List of audit devices
     */
    @GetMapping("/v1/sys/audit")
    List<AuditDevice> getAuditDevices(@RequestHeader("X-Vault-Token") String token);

    /**
     * Enable audit device
     *
     * @param path Audit device path
     * @param auditDevice Audit device configuration
     * @param token Vault token
     * @return Response
     */
    @PutMapping("/v1/sys/audit/{path}")
    Map<String, Object> enableAuditDevice(
            @PathVariable("path") String path,
            @RequestBody AuditDevice auditDevice,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Get transit encryption keys
     *
     * @param token Vault token
     * @return List of keys
     */
    @GetMapping("/v1/transit/keys")
    Map<String, Object> getTransitKeys(@RequestHeader("X-Vault-Token") String token);

    /**
     * Encrypt data
     *
     * @param keyName Key name
     * @param encryptRequest Encryption request
     * @param token Vault token
     * @return Encrypted data
     */
    @PostMapping("/v1/transit/encrypt/{keyName}")
    Map<String, Object> encryptData(
            @PathVariable("keyName") String keyName,
            @RequestBody EncryptRequest encryptRequest,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Decrypt data
     *
     * @param keyName Key name
     * @param decryptRequest Decryption request
     * @param token Vault token
     * @return Decrypted data
     */
    @PostMapping("/v1/transit/decrypt/{keyName}")
    Map<String, Object> decryptData(
            @PathVariable("keyName") String keyName,
            @RequestBody DecryptRequest decryptRequest,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Generate dynamic database credentials
     *
     * @param role Database role
     * @param token Vault token
     * @return Database credentials
     */
    @GetMapping("/v1/database/creds/{role}")
    Map<String, Object> generateDatabaseCredentials(
            @PathVariable("role") String role,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Renew lease
     *
     * @param leaseId Lease ID
     * @param token Vault token
     * @return Renewed lease
     */
    @PutMapping("/v1/sys/renew/{leaseId}")
    Map<String, Object> renewLease(
            @PathVariable("leaseId") String leaseId,
            @RequestHeader("X-Vault-Token") String token
    );

    /**
     * Revoke lease
     *
     * @param leaseId Lease ID
     * @param token Vault token
     * @return Response
     */
    @PutMapping("/v1/sys/revoke/{leaseId}")
    Map<String, Object> revokeLease(
            @PathVariable("leaseId") String leaseId,
            @RequestHeader("X-Vault-Token") String token
    );
}