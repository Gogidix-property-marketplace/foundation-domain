package com.gogidix.ai.fraud.domain.UserManagement.service;

import com.gogidix.ai.fraud.domain.UserManagement.;
import com.gogidix.ai.fraud.domain.UserManagement.Id;
import com.gogidix.ai.fraud.domain.UserManagement.Status;
import com.gogidix.ai.fraud.domain.UserManagement.repository.Repository;
import com.gogidix.ai.fraud.domain.UserManagement.shared.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for Usermanagement business logic.
 * Encapsulates domain-specific business rules and workflows.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainService {

    private final Repository UserManagementRepository;

    /**
     * Creates a new Usermanagement with business validation.
     *
     * @param name        the name of the Usermanagement
     * @param description the description
     * @param createdBy   who is creating this entity
     * @return the created 
     */
    @Transactional
    public  create(String name, String description, String createdBy) {
        log.info("Creating new Usermanagement: {} by {}", name, createdBy);

        // Business validation
        validateNameUniqueness(name);
        validateDescription(description);

         UserManagement = .create(name, description, createdBy);
         saved = UserManagementRepository.save(UserManagement);

        log.info("Successfully created Usermanagement: {} with ID: {}", name, saved.getId());
        return saved;
    }

    /**
     * Updates an existing Usermanagement with business validation.
     *
     * @param id          the ID of the Usermanagement to update
     * @param name        the new name
     * @param description the new description
     * @param updatedBy   who is updating this entity
     * @return the updated 
     * @throws NotFoundException if the Usermanagement is not found
     */
    @Transactional
    public  update(Id id, String name, String description, String updatedBy) {
        log.info("Updating Usermanagement ID: {} by {}", id, updatedBy);

         UserManagement = UserManagementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        // Business validation
        if (!UserManagement.getName().getValue().equals(name)) {
            validateNameUniquenessExcluding(id, name);
        }
        validateDescription(description);

        UserManagement.update(.Name.of(name), description, updatedBy);
         updated = UserManagementRepository.save(UserManagement);

        log.info("Successfully updated Usermanagement: {} with ID: {}", name, updated.getId());
        return updated;
    }

    /**
     * Activates a Usermanagement with business validation.
     *
     * @param id        the ID of the Usermanagement to activate
     * @param activatedBy who is activating this entity
     * @return the activated 
     */
    @Transactional
    public  activate(Id id, String activatedBy) {
        log.info("Activating Usermanagement ID: {} by {}", id, activatedBy);

         UserManagement = UserManagementRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException(id));

        validateActivationAllowed(UserManagement);
        UserManagement.activate(activatedBy);
         activated = UserManagementRepository.save(UserManagement);

        log.info("Successfully activated Usermanagement ID: {}", id);
        return activated;
    }

    /**
     * Deactivates a Usermanagement with business validation.
     *
     * @param id          the ID of the Usermanagement to deactivate
     * @param deactivatedBy who is deactivating this entity
     * @return the deactivated 
     */
    @Transactional
    public  deactivate(Id id, String deactivatedBy) {
        log.info("Deactivating Usermanagement ID: {} by {}", id, deactivatedBy);

         UserManagement = UserManagementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        validateDeactivationAllowed(UserManagement);
        UserManagement.deactivate(deactivatedBy);
         deactivated = UserManagementRepository.save(UserManagement);

        log.info("Successfully deactivated Usermanagement ID: {}", id);
        return deactivated;
    }

    /**
     * Archives a Usermanagement with business validation.
     *
     * @param id        the ID of the Usermanagement to archive
     * @param archivedBy who is archiving this entity
     * @return the archived 
     */
    @Transactional
    public  archive(Id id, String archivedBy) {
        log.info("Archiving Usermanagement ID: {} by {}", id, archivedBy);

         UserManagement = UserManagementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        validateArchivingAllowed(UserManagement);
        UserManagement.archive(archivedBy);
         archived = UserManagementRepository.save(UserManagement);

        log.info("Successfully archived Usermanagement ID: {}", id);
        return archived;
    }

    /**
     * Deletes a Usermanagement with business validation.
     *
     * @param id        the ID of the Usermanagement to delete
     * @param deletedBy who is deleting this entity
     */
    @Transactional
    public void delete(Id id, String deletedBy) {
        log.info("Deleting Usermanagement ID: {} by {}", id, deletedBy);

         UserManagement = UserManagementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        validateDeletionAllowed(UserManagement);

        // Archive instead of hard delete
        UserManagement.archive(deletedBy);
        UserManagementRepository.save(UserManagement);

        log.info("Successfully archived Usermanagement ID: {} (soft delete)", id);
    }

    /**
     * Finds Usermanagement entities using a specification.
     *
     * @param specification the specification to match
     * @return list of matching  entities
     */
    @Transactional(readOnly = true)
    public List<> findBySpecification(Specification<> specification) {
        return UserManagementRepository.findBySpecification(specification);
    }

    /**
     * Finds Usermanagement entities using a specification with pagination.
     *
     * @param specification the specification to match
     * @param page          the page number (0-based)
     * @param size          the page size
     * @return list of matching  entities
     */
    @Transactional(readOnly = true)
    public List<> findBySpecification(Specification<> specification, int page, int size) {
        return UserManagementRepository.findBySpecification(specification, page, size);
    }

    /**
     * Validates that the name is unique.
     *
     * @param name the name to validate
     * @throws DomainException if the name already exists
     */
    private void validateNameUniqueness(String name) {
        if (UserManagementRepository.existsByName(name)) {
            throw new DomainException(" with name '" + name + "' already exists");
        }
    }

    /**
     * Validates that the name is unique, excluding the specified ID.
     *
     * @param id   the ID to exclude from the check
     * @param name the name to validate
     * @throws DomainException if the name already exists
     */
    private void validateNameUniquenessExcluding(Id id, String name) {
        // Find entities with the same name
        List<> existingEntities = UserManagementRepository.findByNameContainingIgnoreCase(name);

        boolean nameExists = existingEntities.stream()
                .anyMatch(entity -> !entity.getId().equals(id) && entity.getName().getValue().equalsIgnoreCase(name));

        if (nameExists) {
            throw new DomainException(" with name '" + name + "' already exists");
        }
    }

    /**
     * Validates the description.
     *
     * @param description the description to validate
     * @throws DomainException if the description is invalid
     */
    private void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new DomainException("Description cannot be null or empty");
        }
        if (description.length() > 500) {
            throw new DomainException("Description cannot exceed 500 characters");
        }
    }

    /**
     * Validates that activation is allowed for the Usermanagement.
     *
     * @param UserManagement the Usermanagement to validate
     * @throws DomainException if activation is not allowed
     */
    private void validateActivationAllowed( UserManagement) {
        if (UserManagement.isActive()) {
            throw new DomainException(" is already active");
        }
        if (UserManagement.isArchived()) {
            throw new DomainException("Cannot activate archived Usermanagement");
        }
    }

    /**
     * Validates that deactivation is allowed for the Usermanagement.
     *
     * @param UserManagement the Usermanagement to validate
     * @throws DomainException if deactivation is not allowed
     */
    private void validateDeactivationAllowed( UserManagement) {
        if (UserManagement.isInactive()) {
            throw new DomainException(" is already inactive");
        }
        if (UserManagement.isArchived()) {
            throw new DomainException("Cannot deactivate archived Usermanagement");
        }
    }

    /**
     * Validates that archiving is allowed for the Usermanagement.
     *
     * @param UserManagement the Usermanagement to validate
     * @throws DomainException if archiving is not allowed
     */
    private void validateArchivingAllowed( UserManagement) {
        if (UserManagement.isArchived()) {
            throw new DomainException(" is already archived");
        }
    }

    /**
     * Validates that deletion is allowed for the Usermanagement.
     *
     * @param UserManagement the Usermanagement to validate
     * @throws DomainException if deletion is not allowed
     */
    private void validateDeletionAllowed( UserManagement) {
        if (UserManagement.isArchived()) {
            log.warn(" {} is already archived", UserManagement.getId());
            return;
        }
        // Add additional business rules for deletion validation
    }
}