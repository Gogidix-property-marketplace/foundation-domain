package com.gogidix.platform.common.persistence.service;

import com.gogidix.platform.common.persistence.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for common persistence operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    /**
     * Save entity with audit information
     */
    @Transactional
    public <T extends BaseEntity> T save(T entity, String updatedBy) {
        if (entity.getId() == null) {
            entity.setCreatedBy(updatedBy);
        }
        entity.setUpdatedBy(updatedBy);
        return entity;
    }

    /**
     * Save entity list with audit information
     */
    @Transactional
    public <T extends BaseEntity> List<T> saveAll(List<T> entities, String updatedBy) {
        entities.forEach(entity -> {
            if (entity.getId() == null) {
                entity.setCreatedBy(updatedBy);
            }
            entity.setUpdatedBy(updatedBy);
        });
        return entities;
    }

    /**
     * Soft delete entity
     */
    @Transactional
    public <T extends BaseEntity, ID> void softDelete(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            ID id, String updatedBy) {
        Optional<T> entity = repository.findById(id);
        if (entity.isPresent()) {
            entity.get().softDelete();
            entity.get().setUpdatedBy(updatedBy);
            repository.save(entity.get());
            log.info("Soft deleted entity: {}", id);
        }
    }

    /**
     * Soft delete multiple entities
     */
    @Transactional
    public <T extends BaseEntity, ID> void softDeleteAll(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            List<ID> ids, String updatedBy) {
        List<T> entities = repository.findByIdInActive(ids);
        entities.forEach(entity -> {
            entity.softDelete();
            entity.setUpdatedBy(updatedBy);
        });
        repository.saveAll(entities);
        log.info("Soft deleted {} entities", entities.size());
    }

    /**
     * Restore soft deleted entity
     */
    @Transactional
    public <T extends BaseEntity, ID> void restore(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            ID id, String updatedBy) {
        Optional<T> entity = repository.findById(id);
        if (entity.isPresent()) {
            entity.get().restore();
            entity.get().setUpdatedBy(updatedBy);
            repository.save(entity.get());
            log.info("Restored entity: {}", id);
        }
    }

    /**
     * Find entity by ID if active
     */
    public <T extends BaseEntity, ID> Optional<T> findByIdActive(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            ID id) {
        return repository.findByIdActive(id);
    }

    /**
     * Find all active entities
     */
    public <T extends BaseEntity> List<T> findAllActive(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ?> repository) {
        return repository.findAllActive();
    }

    /**
     * Find all active entities with pagination
     */
    public <T extends BaseEntity> Page<T> findAllActive(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ?> repository,
            Pageable pageable) {
        return repository.findAllActive(pageable);
    }

    /**
     * Find entities by specification (automatically filters soft-deleted)
     */
    public <T extends BaseEntity> Page<T> findAll(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ?> repository,
            Specification<T> specification, Pageable pageable) {
        Specification<T> activeSpecification = (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("deleted"), false);

        Specification<T> combinedSpecification = Specification.where(activeSpecification).and(specification);
        return repository.findAll(combinedSpecification, pageable);
    }

    /**
     * Count active entities by specification
     */
    public <T extends BaseEntity> long count(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ?> repository,
            Specification<T> specification) {
        Specification<T> activeSpecification = (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("deleted"), false);

        Specification<T> combinedSpecification = Specification.where(activeSpecification).and(specification);
        return repository.count(combinedSpecification);
    }

    /**
     * Check if entity exists and is active
     */
    public <T extends BaseEntity, ID> boolean existsActive(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            ID id) {
        return repository.existsByIdActive(id);
    }

    /**
     * Update audit information for entity
     */
    @Transactional
    public <T extends BaseEntity, ID> void updateAudit(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            ID id, String updatedBy) {
        repository.updateUpdatedById(id, updatedBy, java.time.Instant.now());
    }

    /**
     * Update audit information for multiple entities
     */
    @Transactional
    public <T extends BaseEntity, ID> void updateAuditAll(
            com.gogidix.platform.common.persistence.repository.BaseRepository<T, ID> repository,
            List<ID> ids, String updatedBy) {
        repository.updateUpdatedByIds(ids, updatedBy, java.time.Instant.now());
    }

    /**
     * Execute operation within tenant context
     */
    @Transactional
    public <R> R executeInTenantContext(String tenantId, TenantOperation<R> operation) {
        try {
            // Set tenant context (you can implement ThreadLocal storage)
            TenantContext.setCurrentTenant(tenantId);
            return operation.execute();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Utility interface for tenant-scoped operations
     */
    @FunctionalInterface
    public interface TenantOperation<R> {
        R execute();
    }

    /**
     * ThreadLocal context for tenant management
     */
    public static class TenantContext {
        private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

        public static void setCurrentTenant(String tenantId) {
            CURRENT_TENANT.set(tenantId);
        }

        public static String getCurrentTenant() {
            return CURRENT_TENANT.get();
        }

        public static void clear() {
            CURRENT_TENANT.remove();
        }
    }
}