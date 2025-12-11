package com.gogidix.dashboard.agent.infrastructure.persistence.postgresql.repository;

import com.gogidix.dashboard.agent.domain.UserManagement.;
import com.gogidix.dashboard.agent.domain.UserManagement.Id;
import com.gogidix.dashboard.agent.domain.UserManagement.Status;
import com.gogidix.dashboard.agent.domain.UserManagement.repository.Repository;
import com.gogidix.dashboard.agent.domain.UserManagement.shared.Specification;
import com.gogidix.dashboard.agent.infrastructure.persistence.postgresql.entity.Entity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification as JpaSpecification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PostgreSQL implementation of Repository.
 * Handles persistence operations using JPA and PostgreSQL.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RepositoryImpl implements Repository {

    private final SpringDataRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public  save( UserManagement) {
        log.debug("Saving Usermanagement: {}", UserManagement.getId());

        Entity entity = Entity.fromDomain(UserManagement);
        Entity savedEntity = springDataRepository.save(entity);

        log.debug("Saved Usermanagement entity: {}", savedEntity.getId());
        return savedEntity.toDomain();
    }

    @Override
    @Transactional
    public List<> saveAll(List<> UserManagements) {
        log.debug("Saving {} Usermanagement entities", UserManagements.size());

        List<Entity> entities = UserManagements.stream()
                .map(Entity::fromDomain)
                .collect(Collectors.toList());

        List<Entity> savedEntities = springDataRepository.saveAll(entities);

        log.debug("Saved {} Usermanagement entities", savedEntities.size());
        return savedEntities.stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<> findById(Id id) {
        log.debug("Finding Usermanagement by ID: {}", id);

        return springDataRepository.findById(id.asUuid())
                .map(Entity::toDomain);
    }

    @Override
    public Optional<> findActiveById(Id id) {
        log.debug("Finding active Usermanagement by ID: {}", id);

        return springDataRepository.findByIdAndStatus(id.asUuid(), EntityStatus.ACTIVE)
                .map(Entity::toDomain);
    }

    @Override
    public List<> findByStatus(Status status) {
        log.debug("Finding Usermanagement by status: {}", status);

        EntityStatus entityStatus = EntityStatus.fromDomainStatus(status);
        return springDataRepository.findByStatus(entityStatus).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findByStatusIn(List<Status> statuses) {
        log.debug("Finding Usermanagement by statuses: {}", statuses);

        List<EntityStatus> entityStatuses = statuses.stream()
                .map(EntityStatus::fromDomainStatus)
                .collect(Collectors.toList());

        return springDataRepository.findByStatusIn(entityStatuses).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findBySpecification(Specification<> specification) {
        log.debug("Finding Usermanagement by specification");

        JpaSpecification<Entity> jpaSpec = createJpaSpecification(specification);
        return springDataRepository.findAll(jpaSpec).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findBySpecification(Specification<> specification, int page, int size) {
        log.debug("Finding Usermanagement by specification with pagination: page={}, size={}", page, size);

        JpaSpecification<Entity> jpaSpec = createJpaSpecification(specification);
        PageRequest pageRequest = PageRequest.of(page, size);

        return springDataRepository.findAll(jpaSpec, pageRequest).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findByNameContainingIgnoreCase(String name) {
        log.debug("Finding Usermanagement by name containing: {}", name);

        return springDataRepository.findByNameContainingIgnoreCase(name).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findByCreatedBy(String createdBy) {
        log.debug("Finding Usermanagement created by: {}", createdBy);

        return springDataRepository.findByCreatedBy(createdBy).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findAll() {
        log.debug("Finding all Usermanagement entities");

        return springDataRepository.findAll().stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findAllActive() {
        log.debug("Finding all active Usermanagement entities");

        return springDataRepository.findByStatus(EntityStatus.ACTIVE).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findAll(int page, int size) {
        log.debug("Finding all Usermanagement with pagination: page={}, size={}", page, size);

        PageRequest pageRequest = PageRequest.of(page, size);
        return springDataRepository.findAll(pageRequest).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Id id) {
        log.debug("Checking if Usermanagement exists by ID: {}", id);

        return springDataRepository.existsById(id.asUuid());
    }

    @Override
    public boolean existsByName(String name) {
        log.debug("Checking if Usermanagement exists by name: {}", name);

        return springDataRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public long count() {
        log.debug("Counting all Usermanagement entities");

        return springDataRepository.count();
    }

    @Override
    public long countByStatus(Status status) {
        log.debug("Counting Usermanagement by status: {}", status);

        EntityStatus entityStatus = EntityStatus.fromDomainStatus(status);
        return springDataRepository.countByStatus(entityStatus);
    }

    @Override
    public long countBySpecification(Specification<> specification) {
        log.debug("Counting Usermanagement by specification");

        JpaSpecification<Entity> jpaSpec = createJpaSpecification(specification);
        return springDataRepository.count(jpaSpec);
    }

    @Override
    @Transactional
    public void deleteById(Id id) {
        log.debug("Deleting Usermanagement by ID: {}", id);

        springDataRepository.deleteById(id.asUuid());
    }

    @Override
    @Transactional
    public void delete( UserManagement) {
        log.debug("Deleting Usermanagement: {}", UserManagement.getId());

        springDataRepository.delete(Entity.fromDomain(UserManagement));
    }

    @Override
    @Transactional
    public long deleteByStatus(Status status) {
        log.debug("Deleting Usermanagement by status: {}", status);

        EntityStatus entityStatus = EntityStatus.fromDomainStatus(status);
        return springDataRepository.deleteByStatus(entityStatus);
    }

    @Override
    @Transactional
    public void deleteAll() {
        log.debug("Deleting all Usermanagement entities");

        springDataRepository.deleteAll();
    }

    @Override
    public List<> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Finding Usermanagement created between {} and {}", startDate, endDate);

        return springDataRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Finding Usermanagement updated between {} and {}", startDate, endDate);

        return springDataRepository.findByUpdatedAtBetween(startDate, endDate).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> searchByDescription(String searchTerm) {
        log.debug("Searching Usermanagement by description: {}", searchTerm);

        return springDataRepository.findByDescriptionContainingIgnoreCase(searchTerm).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findByNativeQuery(String nativeQuery, Object... parameters) {
        log.debug("Executing native query: {}", nativeQuery);

        Query query = entityManager.createNativeQuery(nativeQuery, Entity.class);

        for (int i = 0; i < parameters.length; i++) {
            query.setParameter(i + 1, parameters[i]);
        }

        @SuppressWarnings("unchecked")
        List<Entity> entities = query.getResultList();

        return entities.stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByNativeQuery(String nativeQuery, Object... parameters) {
        log.debug("Counting with native query: {}", nativeQuery);

        Query query = entityManager.createNativeQuery(nativeQuery);

        for (int i = 0; i < parameters.length; i++) {
            query.setParameter(i + 1, parameters[i]);
        }

        Object result = query.getSingleResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    @Override
    public List<> findRecentlyCreated(int hours) {
        log.debug("Finding Usermanagement created in last {} hours", hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return springDataRepository.findByCreatedAtAfter(since).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<> findRecentlyUpdated(int hours) {
        log.debug("Finding Usermanagement updated in last {} hours", hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return springDataRepository.findByUpdatedAtAfter(since).stream()
                .map(Entity::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Creates a JPA specification from domain specification.
     *
     * @param domainSpec the domain specification
     * @return JPA specification
     */
    private JpaSpecification<Entity> createJpaSpecification(Specification<> domainSpec) {
        return (root, query, criteriaBuilder) -> {
            // Simple implementation - can be extended for complex specifications
            return criteriaBuilder.conjunction();
        };
    }

    /**
     * Spring Data repository interface.
     */
    public interface SpringDataRepository extends
            org.springframework.data.jpa.repository.JpaRepository<Entity, java.util.UUID>,
            JpaSpecificationExecutor<Entity> {

        Optional<Entity> findByIdAndStatus(java.util.UUID id, EntityStatus status);

        List<Entity> findByStatus(EntityStatus status);

        List<Entity> findByStatusIn(List<EntityStatus> statuses);

        List<Entity> findByNameContainingIgnoreCase(String name);

        List<Entity> findByCreatedBy(String createdBy);

        long countByStatus(EntityStatus status);

        long deleteByStatus(EntityStatus status);

        List<Entity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

        List<Entity> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

        List<Entity> findByDescriptionContainingIgnoreCase(String searchTerm);

        boolean existsByNameIgnoreCase(String name);

        List<Entity> findByCreatedAtAfter(LocalDateTime since);

        List<Entity> findByUpdatedAtAfter(LocalDateTime since);
    }
}