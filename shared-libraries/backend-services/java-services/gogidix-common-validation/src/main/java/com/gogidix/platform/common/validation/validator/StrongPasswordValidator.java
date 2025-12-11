package com.gogidix.platform.common.validation.validator;

import com.gogidix.platform.common.validation.constraint.StrongPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Strong password validator implementation
 */
@Slf4j
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private StrongPassword strongPassword;
    private Pattern uppercasePattern;
    private Pattern lowercasePattern;
    private Pattern digitPattern;
    private Pattern specialCharPattern;

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.strongPassword = constraintAnnotation;
        this.uppercasePattern = Pattern.compile("[A-Z]");
        this.lowercasePattern = Pattern.compile("[a-z]");
        this.digitPattern = Pattern.compile("[0-9]");
        this.specialCharPattern = Pattern.compile("[" + Pattern.quote(constraintAnnotation.specialChars()) + "]");
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }

        // Check minimum length
        if (password.length() < strongPassword.minLength()) {
            addConstraintViolation(context,
                String.format("Password must be at least %d characters long", strongPassword.minLength()));
            return false;
        }

        boolean isValid = true;

        // Check uppercase requirement
        if (strongPassword.requireUppercase() && !uppercasePattern.matcher(password).find()) {
            addConstraintViolation(context, "Password must contain at least one uppercase letter");
            isValid = false;
        }

        // Check lowercase requirement
        if (strongPassword.requireLowercase() && !lowercasePattern.matcher(password).find()) {
            addConstraintViolation(context, "Password must contain at least one lowercase letter");
            isValid = false;
        }

        // Check digit requirement
        if (strongPassword.requireDigits() && !digitPattern.matcher(password).find()) {
            addConstraintViolation(context, "Password must contain at least one digit");
            isValid = false;
        }

        // Check special character requirement
        if (strongPassword.requireSpecialChars()) {
            int specialCharCount = 0;
            for (char c : password.toCharArray()) {
                if (strongPassword.specialChars().indexOf(c) >= 0) {
                    specialCharCount++;
                }
            }

            if (specialCharCount < strongPassword.minSpecialChars()) {
                addConstraintViolation(context,
                    String.format("Password must contain at least %d special character(s)", strongPassword.minSpecialChars()));
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Add constraint violation with custom message
     */
    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}