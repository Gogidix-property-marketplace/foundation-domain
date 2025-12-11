package com.gogidix.infrastructure.auth.application.query;

import com.gogidix.infrastructure.auth.application.dto.ResponseDTO;

import java.util.List;
import java.util.Optional;

/**
 * Query interface for Usermanagemen read operations.
 * Defines the contract for querying Usermanagemen data.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface  {

    /**
     * Finds an Usermanagemen by its ID.
     *
     * @param id the Usermanagemen ID
     * @return optional containing the Usermanagemen if found
     */
    Optional<ResponseDTO> findById(Long id);

    /**
     * Finds Usermanagemen entities by status.
     *
     * @param status the status to filter by
     * @return list of matching Usermanagemen entities
     */
    List<ResponseDTO> findByStatus(String status);

    /**
     * Searches Usermanagemen entities by name.
     *
     * @param searchTerm the search term
     * @return list of matching Usermanagemen entities
     */
    List<ResponseDTO> searchByName(String searchTerm);

    /**
     * Finds all Usermanagemen entities with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of Usermanagemen entities
     */
    List<ResponseDTO> findAll(int page, int size);

    /**
     * Gets the total count of Usermanagemen entities.
     *
     * @return total count
     */
    long count();

    /**
     * Gets the count of Usermanagemen entities by status.
     *
     * @param status the status to count by
     * @return count of entities with the specified status
     */
    long countByStatus(String status);
}