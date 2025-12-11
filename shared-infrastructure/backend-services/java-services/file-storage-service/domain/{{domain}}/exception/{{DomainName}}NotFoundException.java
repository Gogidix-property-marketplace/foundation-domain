package com.gogidix.infrastructure.storage.domain.UserManagement.exception;

import com.gogidix.infrastructure.storage.domain.UserManagement.Id;

/**
 * Exception thrown when a Usermanagement is not found.
 * This is a domain-specific exception for handling missing entities.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class NotFoundException extends RuntimeException {

    private final Id id;

    /**
     * Constructs a new NotFoundException with the specified ID.
     *
     * @param id the ID of the Usermanagement that was not found
     */
    public NotFoundException(Id id) {
        super(" with ID '" + id + "' was not found");
        this.id = id;
    }

    /**
     * Constructs a new NotFoundException with the specified ID and cause.
     *
     * @param id     the ID of the Usermanagement that was not found
     * @param cause  the cause of this exception
     */
    public NotFoundException(Id id, Throwable cause) {
        super(" with ID '" + id + "' was not found", cause);
        this.id = id;
    }

    /**
     * Gets the ID of the Usermanagement that was not found.
     *
     * @return the Usermanagement ID
     */
    public Id getId() {
        return id;
    }

    /**
     * Gets the error code for this exception.
     *
     * @return error code
     */
    public String getErrorCode() {
        return "_NOT_FOUND";
    }
}