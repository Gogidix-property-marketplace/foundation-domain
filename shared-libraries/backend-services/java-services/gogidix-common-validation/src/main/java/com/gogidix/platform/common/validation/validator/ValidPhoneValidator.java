package com.gogidix.platform.common.validation.validator;

import com.gogidix.platform.common.validation.constraint.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Phone number validator implementation
 */
@Slf4j
public class ValidPhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private ValidPhone validPhone;
    private Pattern usPhonePattern;
    private Pattern internationalPhonePattern;
    private Pattern generalPhonePattern;

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        this.validPhone = constraintAnnotation;
        this.usPhonePattern = Pattern.compile("^\\+?1?[ .-]?\\(?([0-9]{3})\\)?[ .-]?([0-9]{3})[ .-]?([0-9]{4})$");
        this.internationalPhonePattern = Pattern.compile("^\\+[1-9]\\d{1,14}$");
        this.generalPhonePattern = Pattern.compile("^[0-9\\-\\s\\(\\)\\+\\.]{7,20}$");
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null) {
            return true;
        }

        String cleanedPhone = phone.trim();

        // Allow blank values if configured
        if (validPhone.allowBlank() && cleanedPhone.isEmpty()) {
            return true;
        }

        // Remove common formatting characters
        cleanedPhone = cleanedPhone.replaceAll("[\\s\\-\\(\\)\\.]+", "");

        // Check minimum length (after removing formatting)
        if (cleanedPhone.length() < 7) {
            return false;
        }

        // Check maximum length
        if (cleanedPhone.length() > 20) {
            return false;
        }

        // Validate based on country
        switch (validPhone.defaultCountry().toUpperCase()) {
            case "US":
                return isValidUSPhone(phone.trim());
            default:
                return isValidInternationalPhone(cleanedPhone);
        }
    }

    /**
     * Validate US phone number
     */
    private boolean isValidUSPhone(String phone) {
        return usPhonePattern.matcher(phone).matches() ||
               (phone.matches("^[0-9]{10}$") && !phone.startsWith("1"));
    }

    /**
     * Validate international phone number
     */
    private boolean isValidInternationalPhone(String phone) {
        // Check if it starts with + (international format)
        if (phone.startsWith("+")) {
            return internationalPhonePattern.matcher(phone).matches();
        }

        // General validation for phone without country code
        return generalPhonePattern.matcher(phone).matches();
    }
}