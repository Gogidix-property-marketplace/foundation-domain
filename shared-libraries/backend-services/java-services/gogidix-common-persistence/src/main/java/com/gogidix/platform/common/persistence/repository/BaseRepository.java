package com.gogidix.platform.common.persistence.repository;

import com.gogidix.platform.common.persistence.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface providing common CRUD operations
 *
 * @param <T> Entity type
 * @param <ID> Entity ID type
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Find entity by ID if it's not soft deleted
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    Optional<T> findByIdActive(ID id);

    /**
     * Find all active entities (not soft deleted)
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    List<T> findAllActive();

    /**
     * Find all active entities with pagination
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    Page<T> findAllActive(Pageable pageable);

    /**
     * Soft delete entity by ID
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.updatedAt = :updatedAt, e.updatedBy = :updatedBy WHERE e.id = :id")
    void softDeleteById(ID id, Instant updatedAt, String updatedBy);

    /**
     * Soft delete multiple entities by IDs
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.updatedAt = :updatedAt, e.updatedBy = :updatedBy WHERE e.id IN :ids")
    void softDeleteByIds(List<ID> ids, Instant updatedAt, String updatedBy);

    /**
     * Restore soft deleted entity by ID
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = false, e.updatedAt = :updatedAt, e.updatedBy = :updatedBy WHERE e.id = :id")
    void restoreById(ID id, Instant updatedAt, String updatedBy);

    /**
     * Find entities by tenant ID
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deleted = false")
    List<T> findByTenantId(String tenantId);

    /**
     * Find entities by tenant ID with pagination
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deleted = false")
    Page<T> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find entities by created by user
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdBy = :createdBy AND e.deleted = false")
    List<T> findByCreatedBy(String createdBy);

    /**
     * Find entities by created by user with pagination
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdBy = :createdBy AND e.deleted = false")
    Page<T> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find entities created within date range
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate AND e.deleted = false")
    List<T> findByCreatedAtBetween(Instant startDate, Instant endDate);

    /**
     * Find entities updated within date range
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.updatedAt BETWEEN :startDate AND :endDate AND e.deleted = false")
    List<T> findByUpdatedAtBetween(Instant startDate, Instant endDate);

    /**
     * Count active entities
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = false")
    long countActive();

    /**
     * Count entities by tenant ID
     */
    @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.tenantId = :tenantId AND e.deleted = false")
    long countByTenantId(String tenantId);

    /**
     * Check if entity exists by ID and is not soft deleted
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    boolean existsByIdActive(ID id);

    /**
     * Find entities by multiple IDs (active only)
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id IN :ids AND e.deleted = false")
    List<T> findByIdInActive(List<ID> ids);

    /**
     * Update updated by information for entity
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.updatedBy = :updatedBy, e.updatedAt = :updatedAt WHERE e.id = :id")
    void updateUpdatedById(ID id, String updatedBy, Instant updatedAt);

    /**
     * Update updated by information for multiple entities
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.updatedBy = :updatedBy, e.updatedAt = :updatedAt WHERE e.id IN :ids")
    void updateUpdatedByIds(List<ID> ids, String updatedBy, Instant updatedAt);
}