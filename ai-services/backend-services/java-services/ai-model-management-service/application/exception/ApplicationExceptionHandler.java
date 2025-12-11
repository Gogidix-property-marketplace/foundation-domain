package com.gogidix.ai.model.application.exception;

import com.gogidix.ai.model.web.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for application layer exceptions.
 * Handles application-specific exceptions and returns standardized error responses.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class ApplicationExceptionHandler {

    /**
     * Handles application exceptions.
     *
     * @param ex the application exception
     * @return error response
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException ex) {
        log.error("Application exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Application Error")
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation exceptions.
     *
     * @param ex the validation exception
     * @return error response
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.error("Validation exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Validation Error")
                .message(ex.getMessage())
                .errorCode("VALIDATION_ERROR")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex the resource not found exception
     * @return error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Resource Not Found")
                .message(ex.getMessage())
                .errorCode("RESOURCE_NOT_FOUND")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles business rule violations.
     *
     * @param ex the business rule violation exception
     * @return error response
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(BusinessRuleViolationException ex) {
        log.error("Business rule violation: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("Business Rule Violation")
                .message(ex.getMessage())
                .errorCode("BUSINESS_RULE_VIOLATION")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}