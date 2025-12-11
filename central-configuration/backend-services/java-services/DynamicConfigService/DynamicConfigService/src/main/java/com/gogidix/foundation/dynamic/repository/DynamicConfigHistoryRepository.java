package com.gogidix.foundation.dynamic.repository;

import com.gogidix.foundation.dynamic.entity.DynamicConfigHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DynamicConfigHistoryRepository extends JpaRepository<DynamicConfigHistory, Long> {

    Page<DynamicConfigHistory> findByConfigIdOrderByCreatedAtDesc(Long configId, Pageable pageable);

    List<DynamicConfigHistory> findByConfigIdOrderByCreatedAtDesc(Long configId);

    List<DynamicConfigHistory> findByConfigKeyOrderByCreatedAtDesc(String configKey);

    Page<DynamicConfigHistory> findByChangedByOrderByCreatedAtDesc(String changedBy, Pageable pageable);

    @Query("SELECT dch FROM DynamicConfigHistory dch WHERE " +
           "(:configId IS NULL OR dch.configId = :configId) AND " +
           "(:configKey IS NULL OR LOWER(dch.configKey) LIKE LOWER(CONCAT('%', :configKey, '%'))) AND " +
           "(:changedBy IS NULL OR dch.changedBy = :changedBy) AND " +
           "(:fromDate IS NULL OR dch.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR dch.createdAt <= :toDate)")
    Page<DynamicConfigHistory> searchHistory(
            @Param("configId") Long configId,
            @Param("configKey") String configKey,
            @Param("changedBy") String changedBy,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT COUNT(dch) FROM DynamicConfigHistory dch WHERE dch.configId = :configId")
    long countChangesSince(@Param("configId") Long configId);

    List<DynamicConfigHistory> findTop10ByConfigIdOrderByCreatedAtDesc(Long configId);

    @Query("SELECT COUNT(dch) FROM DynamicConfigHistory dch WHERE dch.createdAt >= :since")
    long countChangesSince(@Param("since") LocalDateTime since);

    @Query("SELECT dch FROM DynamicConfigHistory dch WHERE dch.createdAt >= :since ORDER BY dch.createdAt DESC")
    List<DynamicConfigHistory> findRecentChanges(@Param("since") LocalDateTime since);
}