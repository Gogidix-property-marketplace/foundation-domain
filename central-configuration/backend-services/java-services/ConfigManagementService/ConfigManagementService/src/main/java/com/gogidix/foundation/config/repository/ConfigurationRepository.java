package com.gogidix.foundation.config.repository;

import com.gogidix.foundation.config.entity.Configuration;
import com.gogidix.foundation.config.enums.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    Optional<Configuration> findByKeyAndEnvironment(String key, Environment environment);

    List<Configuration> findByKeyContainingIgnoreCase(String key);

    List<Configuration> findByEnvironment(Environment environment);

    List<Configuration> findByApplicationName(String applicationName);

    List<Configuration> findByEnvironmentAndApplicationName(Environment environment, String applicationName);

    Page<Configuration> findByEnvironmentAndApplicationName(Environment environment, String applicationName, Pageable pageable);

    @Query("SELECT c FROM Configuration c WHERE " +
           "(:key IS NULL OR c.key LIKE %:key%) AND " +
           "(:environment IS NULL OR c.environment = :environment) AND " +
           "(:applicationName IS NULL OR c.applicationName = :applicationName) AND " +
           "c.active = true")
    Page<Configuration> searchConfigurations(@Param("key") String key,
                                            @Param("environment") Environment environment,
                                            @Param("applicationName") String applicationName,
                                            Pageable pageable);

    @Query("SELECT DISTINCT c.applicationName FROM Configuration c WHERE c.applicationName IS NOT NULL")
    List<String> findAllApplicationNames();

    @Modifying
    @Query("UPDATE Configuration c SET c.active = false WHERE c.id = :id")
    int deactivateConfiguration(@Param("id") Long id);

    @Query("SELECT c FROM Configuration c WHERE c.active = true AND c.environment = :environment")
    List<Configuration> findActiveByEnvironment(@Param("environment") Environment environment);

    boolean existsByKeyAndEnvironment(String key, Environment environment);

    @Query("SELECT COUNT(c) FROM Configuration c WHERE c.environment = :environment AND c.active = true")
    long countActiveByEnvironment(@Param("environment") Environment environment);
}