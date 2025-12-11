package com.gogidix.ai.sentiment.domain.UserManagement.repository;

import com.gogidix.ai.sentiment.domain.UserManagement.;
import com.gogidix.ai.sentiment.domain.UserManagement.Id;
import com.gogidix.ai.sentiment.domain.UserManagement.Status;
import com.gogidix.ai.sentiment.domain.UserManagement.shared.Specification;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for  entities.
 * Defines the contract for Usermanagement persistence operations.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface Repository {

    /**
     * Saves a Usermanagement to the repository.
     *
     * @param UserManagement the Usermanagement to save
     * @return the saved Usermanagement
     */
     save( UserManagement);

    /**
     * Saves multiple Usermanagement entities in a single transaction.
     *
     * @param UserManagements the list of Usermanagement entities to save
     * @return the list of saved Usermanagement entities
     */
    List<> saveAll(List<> UserManagements);

    /**
     * Finds a Usermanagement by its ID.
     *
     * @param id the ID of the Usermanagement
     * @return Optional containing the Usermanagement if found, empty otherwise
     */
    Optional<> findById(Id id);

    /**
     * Finds a Usermanagement by its ID and ensures it's not deleted.
     *
     * @param id the ID of the Usermanagement
     * @return Optional containing the Usermanagement if found and not deleted
     */
    Optional<> findActiveById(Id id);

    /**
     * Finds Usermanagement entities by their status.
     *
     * @param status the status to filter by
     * @return list of Usermanagement entities with the specified status
     */
    List<> findByStatus(Status status);

    /**
     * Finds Usermanagement entities by multiple statuses.
     *
     * @param statuses the list of statuses to filter by
     * @return list of Usermanagement entities with any of the specified statuses
     */
    List<> findByStatusIn(List<Status> statuses);

    /**
     * Finds Usermanagement entities matching the given specification.
     *
     * @param specification the specification to match
     * @return list of matching Usermanagement entities
     */
    List<> findBySpecification(Specification<> specification);

    /**
     * Finds Usermanagement entities matching the given specification with pagination.
     *
     * @param specification the specification to match
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of matching Usermanagement entities
     */
    List<> findBySpecification(Specification<> specification, int page, int size);

    /**
     * Finds Usermanagement entities by name (case-insensitive partial match).
     *
     * @param name the name to search for
     * @return list of matching Usermanagement entities
     */
    List<> findByNameContainingIgnoreCase(String name);

    /**
     * Finds Usermanagement entities created by a specific user.
     *
     * @param createdBy the user who created the Usermanagement entities
     * @return list of Usermanagement entities created by the specified user
     */
    List<> findByCreatedBy(String createdBy);

    /**
     * Returns all Usermanagement entities.
     *
     * @return list of all Usermanagement entities
     */
    List<> findAll();

    /**
     * Returns all active Usermanagement entities.
     *
     * @return list of active Usermanagement entities
     */
    List<> findAllActive();

    /**
     * Returns all Usermanagement entities with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of Usermanagement entities for the specified page
     */
    List<> findAll(int page, int size);

    /**
     * Checks if a Usermanagement exists with the given ID.
     *
     * @param id the ID to check
     * @return true if exists, false otherwise
     */
    boolean existsById(Id id);

    /**
     * Checks if a Usermanagement exists with the given name.
     *
     * @param name the name to check
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Counts the total number of Usermanagement entities.
     *
     * @return total count
     */
    long count();

    /**
     * Counts Usermanagement entities by status.
     *
     * @param status the status to count by
     * @return count of entities with the specified status
     */
    long countByStatus(Status status);

    /**
     * Counts Usermanagement entities matching the given specification.
     *
     * @param specification the specification to match
     * @return count of matching entities
     */
    long countBySpecification(Specification<> specification);

    /**
     * Deletes a Usermanagement by its ID.
     *
     * @param id the ID of the Usermanagement to delete
     */
    void deleteById(Id id);

    /**
     * Deletes the given Usermanagement entity.
     *
     * @param UserManagement the Usermanagement to delete
     */
    void delete( UserManagement);

    /**
     * Deletes Usermanagement entities by their status.
     *
     * @param status the status to delete by
     * @return the number of entities deleted
     */
    long deleteByStatus(Status status);

    /**
     * Deletes all Usermanagement entities.
     */
    void deleteAll();

    /**
     * Finds Usermanagement entities that were created within the specified date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of Usermanagement entities created within the date range
     */
    List<> findByCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Finds Usermanagement entities that were updated within the specified date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of Usermanagement entities updated within the date range
     */
    List<> findByUpdatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Finds Usermanagement entities with names matching the given pattern using full-text search.
     *
     * @param searchTerm the search term
     * @return list of matching Usermanagement entities
     */
    List<> searchByDescription(String searchTerm);

    /**
     * Finds Usermanagement entities using a native SQL query.
     *
     * @param nativeQuery the native SQL query
     * @param parameters the query parameters
     * @return list of matching Usermanagement entities
     */
    List<> findByNativeQuery(String nativeQuery, Object... parameters);

    /**
     * Executes a custom query and returns the result count.
     *
     * @param nativeQuery the native SQL query
     * @param parameters the query parameters
     * @return count of results
     */
    long countByNativeQuery(String nativeQuery, Object... parameters);

    /**
     * Finds recently created Usermanagement entities.
     *
     * @param hours the number of hours to look back
     * @return list of recently created Usermanagement entities
     */
    List<> findRecentlyCreated(int hours);

    /**
     * Finds recently updated Usermanagement entities.
     *
     * @param hours the number of hours to look back
     * @return list of recently updated Usermanagement entities
     */
    List<> findRecentlyUpdated(int hours);
}