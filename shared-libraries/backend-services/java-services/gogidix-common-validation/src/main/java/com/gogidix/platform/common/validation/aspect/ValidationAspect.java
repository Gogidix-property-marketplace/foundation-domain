package com.gogidix.platform.common.validation.aspect;

import com.gogidix.platform.common.validation.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import jakarta.validation.Valid;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aspect for automatic validation of method parameters annotated with @Valid
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ValidationAspect {

    private final ValidationService validationService;

    /**
     * Validate method parameters annotated with @Valid
     */
    @Before("execution(* *(.., @Valid (*), ..))")
    public void validateMethodParameters(JoinPoint joinPoint) {
        log.debug("Validating method parameters for: {}", joinPoint.getSignature().getName());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        List<ValidationError> validationErrors = new ArrayList<>();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];

            // Check if parameter is annotated with @Valid
            if (parameter.isAnnotationPresent(Valid.class) && arg != null) {
                try {
                    validationService.validateOrThrow(arg);
                } catch (ValidationService.ValidationException e) {
                    validationErrors.add(new ValidationError(
                            parameter.getName(),
                            "Parameter validation failed: " + e.getMessage(),
                            arg
                    ));
                }
            }

            // Check for custom validation annotations
            Annotation[] annotations = parameter.getAnnotations();
            for (Annotation annotation : annotations) {
                if (isValidationAnnotation(annotation)) {
                    ValidationResult result = validateWithAnnotation(arg, annotation);
                    if (!result.isValid()) {
                        result.errors.forEach(validationErrors::add);
                    }
                }
            }
        }

        if (!validationErrors.isEmpty()) {
            List<ValidationService.ValidationError> serviceErrors = validationErrors.stream()
                .map(ve -> new ValidationService.ValidationError(
                    ve.field(),
                    ve.message(),
                    ve.invalidValue()))
                .collect(Collectors.toList());
            throw new ValidationService.ValidationException(serviceErrors);
        }
    }

    /**
     * Check if annotation is a validation annotation
     */
    private boolean isValidationAnnotation(Annotation annotation) {
        return annotation.annotationType().isAnnotationPresent(jakarta.validation.Constraint.class);
    }

    /**
     * Validate object with specific annotation
     */
    private ValidationResult validateWithAnnotation(Object obj, Annotation annotation) {
        // This is a simplified implementation
        // In a real scenario, you would use the validator to validate against specific constraints
        return new ValidationResult(List.of());
    }

    /**
     * Validation result holder
     */
    private record ValidationResult(boolean isValid, List<ValidationError> errors) {
        public ValidationResult(List<ValidationError> errors) {
            this(errors.isEmpty(), errors);
        }
    }

    /**
     * Validation error holder - using same structure as ValidationService
     */
    private record ValidationError(String field, String message, String invalidValue) {
        public ValidationError(String field, String message, Object invalidValue) {
            this(field, message, invalidValue != null ? invalidValue.toString() : "null");
        }
    }
}