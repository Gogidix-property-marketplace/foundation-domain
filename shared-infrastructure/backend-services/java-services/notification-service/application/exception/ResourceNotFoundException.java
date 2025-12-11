package com.gogidix.infrastructure.notification.application.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Represents resource not found errors in the application layer.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
public class ResourceNotFoundException extends ApplicationException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Constructs a new ResourceNotFoundException with the specified message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Constructs a new ResourceNotFoundException for a specific resource.
     *
     * @param resourceType the type of resource
     * @param resourceId   the ID of the resource
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId), "RESOURCE_NOT_FOUND");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Gets the resource type.
     *
     * @return the resource type
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the resource ID.
     *
     * @return the resource ID
     */
    public String getResourceId() {
        return resourceId;
    }
}