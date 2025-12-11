package com.gogidix.platform.common.exception;

/**
 * Exception thrown when a requested resource is not found
 * Typically results in HTTP 404 response
 */
public class ResourceNotFoundException extends GogidixException {

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, 404);
    }

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with id '%s' not found", resourceType, resourceId),
              404);
    }

    public ResourceNotFoundException(String resourceType, String fieldName, Object fieldValue) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with %s '%s' not found", resourceType, fieldName, fieldValue),
              404);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, 404, cause);
    }
}