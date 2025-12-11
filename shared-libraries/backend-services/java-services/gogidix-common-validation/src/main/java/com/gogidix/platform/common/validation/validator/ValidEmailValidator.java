package com.gogidix.platform.common.validation.validator;

import com.gogidix.platform.common.validation.constraint.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Email validator implementation
 */
@Slf4j
public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private ValidEmail validEmail;
    private Pattern pattern;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.validEmail = constraintAnnotation;
        this.pattern = Pattern.compile(constraintAnnotation.pattern());
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) {
            return true;
        }

        String trimmedEmail = email.trim();

        // Allow blank values if configured
        if (validEmail.allowBlank() && trimmedEmail.isEmpty()) {
            return true;
        }

        // Basic format validation using regex
        if (!pattern.matcher(trimmedEmail).matches()) {
            return false;
        }

        // Additional email format validation
        if (!isValidEmailFormat(trimmedEmail)) {
            return false;
        }

        // Optional MX record check (DNS validation)
        if (validEmail.checkMX()) {
            try {
                String domain = trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
                if (!hasMXRecords(domain)) {
                    log.debug("No MX records found for domain: {}", domain);
                    return false;
                }
            } catch (Exception e) {
                log.warn("Error checking MX records for email {}: {}", email, e.getMessage());
                // Don't fail validation on DNS errors
                return true;
            }
        }

        return true;
    }

    /**
     * Additional email format validation
     */
    private boolean isValidEmailFormat(String email) {
        // Check for valid email format patterns
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Local part validation
        if (localPart.isEmpty() || localPart.length() > 64) {
            return false;
        }

        // Domain part validation
        if (domainPart.isEmpty() || domainPart.length() > 255) {
            return false;
        }

        // Check if domain has at least one dot
        if (!domainPart.contains(".")) {
            return false;
        }

        // Basic character validation
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Check if domain has MX records
     */
    private boolean hasMXRecords(String domain) {
        try {
            // This would require DNS lookup implementation
            // For now, return true to avoid external dependencies
            // In production, consider using dnsjava library
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}