package com.gogidix.infrastructure.circuit.application.usecase;

import com.gogidix.infrastructure.circuit.application.dto.DTO;
import com.gogidix.infrastructure.circuit.application.dto.CreateDTO;
import com.gogidix.infrastructure.circuit.application.dto.UpdateDTO;
import com.gogidix.infrastructure.circuit.application.dto.ResponseDTO;

import java.util.List;
import java.util.Optional;

/**
 * Use case interface for Usermanagemen operations.
 * Defines the contract for Usermanagemen business logic at the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public interface  {

    /**
     * Creates a new Usermanagemen.
     *
     * @param createDTO the DTO containing creation data
     * @return the created Usermanagemen
     */
    ResponseDTO create(CreateDTO createDTO);

    /**
     * Updates an existing Usermanagemen.
     *
     * @param id        the ID of the Usermanagemen to update
     * @param updateDTO the DTO containing update data
     * @return the updated Usermanagemen
     */
    ResponseDTO update(Long id, UpdateDTO updateDTO);

    /**
     * Finds an Usermanagemen by its ID.
     *
     * @param id the ID of the Usermanagemen
     * @return Optional containing the Usermanagemen if found
     */
    Optional<ResponseDTO> findById(Long id);

    /**
     * Finds Usermanagemen entities by status.
     *
     * @param status the status to filter by
     * @return list of Usermanagemen entities
     */
    List<ResponseDTO> findByStatus(String status);

    /**
     * Finds all Usermanagemen entities.
     *
     * @return list of all Usermanagemen entities
     */
    List<ResponseDTO> findAll();

    /**
     * Finds Usermanagemen entities with pagination.
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of Usermanagemen entities for the specified page
     */
    List<ResponseDTO> findAll(int page, int size);

    /**
     * Searches Usermanagemen entities by name.
     *
     * @param searchTerm the search term
     * @return list of matching Usermanagemen entities
     */
    List<ResponseDTO> searchByName(String searchTerm);

    /**
     * Activates an Usermanagemen.
     *
     * @param id the ID of the Usermanagemen to activate
     */
    void activate(Long id);

    /**
     * Deactivates an Usermanagemen.
     *
     * @param id the ID of the Usermanagemen to deactivate
     */
    void deactivate(Long id);

    /**
     * Deletes an Usermanagemen.
     *
     * @param id the ID of the Usermanagemen to delete
     */
    void delete(Long id);

    /**
     * Gets the total count of Usermanagemen entities.
     *
     * @return total count
     */
    long getCount();

    /**
     * Gets the count of Usermanagemen entities by status.
     *
     * @param status the status to count by
     * @return count of entities with the specified status
     */
    long getCountByStatus(String status);
}