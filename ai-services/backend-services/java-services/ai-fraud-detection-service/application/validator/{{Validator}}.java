package com.gogidix.ai.fraud.application.validator;

import com.gogidix.ai.fraud.application.dto.DTO;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for Usermanagemen DTOs.
 * Provides validation logic for Usermanagemen data transfer objects.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Component
public class  {

    private final jakarta.validation.Validator validator;

    public (Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates Usermanagemen DTO.
     *
     * @param UsermanagemenDTO the DTO to validate
     * @return validation result with errors
     */
    public ValidationResult validate(DTO UsermanagemenDTO) {
        Set<ConstraintViolation<DTO>> violations = validator.validate(UsermanagemenDTO);

        return new ValidationResult(
                violations.isEmpty(),
                violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.toList())
        );
    }

    /**
     * Validation result container.
     */
    public record ValidationResult(boolean isValid, java.util.List<String> errors) {}
}