package com.gogidix.infrastructure.serviceregistry.web.validator;

import com.gogidix.infrastructure.serviceregistry.application.dto.CreateDTO;
import com.gogidix.infrastructure.serviceregistry.application.dto.UpdateDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;

/**
 * Validator for Usermanagemen DTOs.
 * Provides comprehensive validation rules for Usermanagemen data.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Component
public class Validator implements Validator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public boolean supports(Class<?> clazz) {
        return CreateDTO.class.isAssignableFrom(clazz) ||
               UpdateDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof CreateDTO) {
            validateCreate((CreateDTO) target, errors);
        } else if (target instanceof UpdateDTO) {
            validateUpdate((UpdateDTO) target, errors);
        }
    }

    /**
     * Validates CreateDTO.
     *
     * @param dto   the DTO to validate
     * @param errors validation errors
     */
    private void validateCreate(CreateDTO dto, Errors errors) {
        validateName(dto.getName(), errors);
        validateDescription(dto.getDescription(), errors);
    }

    /**
     * Validates UpdateDTO.
     *
     * @param dto   the DTO to validate
     * @param errors validation errors
     */
    private void validateUpdate(UpdateDTO dto, Errors errors) {
        validateName(dto.getName(), errors);
        validateDescription(dto.getDescription(), errors);
    }

    /**
     * Validates the name field.
     *
     * @param name  the name to validate
     * @param errors validation errors
     */
    private void validateName(String name, Errors errors) {
        if (name == null || name.trim().isEmpty()) {
            errors.rejectValue("name", "field.required", "Name is required");
            return;
        }

        if (name.length() < 2) {
            errors.rejectValue("name", "field.min.length", "Name must be at least 2 characters long");
        }

        if (name.length() > 100) {
            errors.rejectValue("name", "field.max.length", "Name cannot exceed 100 characters");
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            errors.rejectValue("name", "field.pattern", "Name contains invalid characters");
        }

        if (name.toLowerCase().contains("admin") ||
            name.toLowerCase().contains("system") ||
            name.toLowerCase().contains("root")) {
            errors.rejectValue("name", "field.forbidden", "Name contains forbidden words");
        }
    }

    /**
     * Validates the description field.
     *
     * @param description the description to validate
     * @param errors       validation errors
     */
    private void validateDescription(String description, Errors errors) {
        if (description == null || description.trim().isEmpty()) {
            errors.rejectValue("description", "field.required", "Description is required");
            return;
        }

        if (description.length() < 10) {
            errors.rejectValue("description", "field.min.length", "Description must be at least 10 characters long");
        }

        if (description.length() > 500) {
            errors.rejectValue("description", "field.max.length", "Description cannot exceed 500 characters");
        }
    }

    /**
     * Validates email field (if applicable).
     *
     * @param email the email to validate
     * @param fieldName the field name
     * @param errors validation errors
     */
    protected void validateEmail(String email, String fieldName, Errors errors) {
        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.rejectValue(fieldName, "field.invalid.email", "Invalid email format");
            }
        }
    }

    /**
     * Validates phone number field (if applicable).
     *
     * @param phone     the phone number to validate
     * @param fieldName the field name
     * @param errors    validation errors
     */
    protected void validatePhoneNumber(String phone, String fieldName, Errors errors) {
        if (phone != null && !phone.trim().isEmpty()) {
            // Remove all non-digit characters
            String digitsOnly = phone.replaceAll("[^0-9]", "");

            if (digitsOnly.length() < 10 || digitsOnly.length() > 15) {
                errors.rejectValue(fieldName, "field.invalid.phone", "Invalid phone number format");
            }
        }
    }

    /**
     * Validates numeric field.
     *
     * @param value     the numeric value to validate
     * @param fieldName the field name
     * @param minValue  the minimum allowed value
     * @param maxValue  the maximum allowed value
     * @param errors    validation errors
     */
    protected void validateNumericRange(Double value, String fieldName, Double minValue, Double maxValue, Errors errors) {
        if (value != null) {
            if (minValue != null && value < minValue) {
                errors.rejectValue(fieldName, "field.min.value",
                    String.format("Value must be at least %.2f", minValue));
            }
            if (maxValue != null && value > maxValue) {
                errors.rejectValue(fieldName, "field.max.value",
                    String.format("Value must not exceed %.2f", maxValue));
            }
        }
    }

    /**
     * Validates enum field.
     *
     * @param value     the enum value to validate
     * @param fieldName the field name
     * @param validValues the list of valid values
     * @param errors    validation errors
     */
    protected void validateEnum(String value, String fieldName, String[] validValues, Errors errors) {
        if (value != null && !value.trim().isEmpty()) {
            boolean isValid = false;
            for (String validValue : validValues) {
                if (validValue.equalsIgnoreCase(value.trim())) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                errors.rejectValue(fieldName, "field.invalid.enum",
                    String.format("Value must be one of: %s", String.join(", ", validValues)));
            }
        }
    }
}