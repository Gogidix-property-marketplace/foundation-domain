package com.gogidix.foundation.dynamic.repository;

import com.gogidix.foundation.dynamic.entity.DynamicConfig;
import com.gogidix.foundation.dynamic.enums.ConfigScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicConfigRepository extends JpaRepository<DynamicConfig, Long> {

    Optional<DynamicConfig> findByConfigKeyAndScope(String configKey, ConfigScope scope);

    List<DynamicConfig> findByConfigKeyAndScopeAndActiveTrue(String configKey, ConfigScope scope);

    Optional<DynamicConfig> findByConfigKeyAndScopeAndApplicationName(
            String configKey, ConfigScope scope, String applicationName);

    List<DynamicConfig> findByScopeAndActiveTrue(ConfigScope scope);

    List<DynamicConfig> findByApplicationNameAndActiveTrue(String applicationName);

    List<DynamicConfig> findByServiceNameAndActiveTrue(String serviceName);

    List<DynamicConfig> findByEnvironmentAndActiveTrue(String environment);

    List<DynamicConfig> findByUserIdAndActiveTrue(String userId);

    Page<DynamicConfig> findByConfigKeyContainingIgnoreCase(String configKey, Pageable pageable);

    @Query("SELECT dc FROM DynamicConfig dc WHERE " +
           "(:configKey IS NULL OR LOWER(dc.configKey) LIKE LOWER(CONCAT('%', :configKey, '%'))) AND " +
           "(:scope IS NULL OR dc.scope = :scope) AND " +
           "(:applicationName IS NULL OR dc.applicationName = :applicationName) AND " +
           "(:serviceName IS NULL OR dc.serviceName = :serviceName) AND " +
           "(:environment IS NULL OR dc.environment = :environment) AND " +
           "(:active IS NULL OR dc.active = :active)")
    Page<DynamicConfig> searchDynamicConfigs(
            @Param("configKey") String configKey,
            @Param("scope") ConfigScope scope,
            @Param("applicationName") String applicationName,
            @Param("serviceName") String serviceName,
            @Param("environment") String environment,
            @Param("active") Boolean active,
            Pageable pageable);

    List<String> findAllApplicationNames();

    List<String> findAllServiceNames();

    List<String> findAllEnvironments();

    boolean existsByConfigKeyAndScope(String configKey, ConfigScope scope);

    @Query("SELECT COUNT(dc) FROM DynamicConfig dc WHERE dc.active = true")
    long countActiveConfigs();

    @Query("SELECT COUNT(dc) FROM DynamicConfig dc WHERE dc.encrypted = true")
    long countEncryptedConfigs();

    @Query("SELECT COUNT(dc) FROM DynamicConfig dc WHERE dc.requiresRestart = true")
    long countConfigsRequiringRestart();
}