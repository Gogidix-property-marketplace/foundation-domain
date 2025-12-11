package com.gogidix.foundation.config.repository;

import com.gogidix.foundation.config.entity.ConfigurationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConfigurationHistoryRepository extends JpaRepository<ConfigurationHistory, Long> {

    List<ConfigurationHistory> findByConfigurationIdOrderByCreatedAtDesc(Long configurationId);

    Page<ConfigurationHistory> findByConfigurationIdOrderByCreatedAtDesc(Long configurationId, Pageable pageable);

    @Query("SELECT ch FROM ConfigurationHistory ch WHERE " +
           "ch.configurationId = :configId AND " +
           "ch.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ch.createdAt DESC")
    List<ConfigurationHistory> findByConfigurationIdAndDateRange(
            @Param("configId") Long configId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT ch FROM ConfigurationHistory ch WHERE " +
           "ch.configKey = :configKey AND ch.environment = :environment " +
           "ORDER BY ch.createdAt DESC")
    List<ConfigurationHistory> findByConfigKeyAndEnvironment(
            @Param("configKey") String configKey,
            @Param("environment") String environment);

    @Query("SELECT ch FROM ConfigurationHistory ch WHERE " +
           "ch.changedBy = :changedBy " +
           "ORDER BY ch.createdAt DESC")
    Page<ConfigurationHistory> findByChangedBy(@Param("changedBy") String changedBy, Pageable pageable);

    @Query("SELECT COUNT(ch) FROM ConfigurationHistory ch WHERE " +
           "ch.configurationId = :configId AND " +
           "ch.createdAt >= :since")
    long countChangesSince(@Param("configId") Long configId, @Param("since") LocalDateTime since);

    @Query("SELECT ch FROM ConfigurationHistory ch WHERE " +
           "ch.changeType = :changeType " +
           "ORDER BY ch.createdAt DESC")
    List<ConfigurationHistory> findByChangeType(@Param("changeType") String changeType);
}