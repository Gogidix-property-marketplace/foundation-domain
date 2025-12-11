package com.gogidix.ai.chatbot.application.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Aspect for implementing retry logic on methods.
 * Provides automatic retry functionality for transient failures.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class RetryAspect {

    /**
     * Applies retry logic to external service calls.
     *
     * @param joinPoint the join point
     * @return method result
     * @throws Throwable if method execution fails after all retries
     */
    @Around("@annotation(org.springframework.retry.annotation.Retryable)")
    @Retryable(
        value = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Object retryExternalServiceCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        int attempt = 1;
        while (true) {
            try {
                log.debug("Executing {}.{} (attempt {})", className, methodName, attempt);
                Object result = joinPoint.proceed();
                log.debug("Successfully executed {}.{}", className, methodName);
                return result;
            } catch (Exception e) {
                log.warn("Attempt {} failed for {}.{}: {}",
                        attempt, className, methodName, e.getMessage());

                if (attempt >= 3) {
                    log.error("All retry attempts exhausted for {}.{}", className, methodName);
                    throw e;
                }

                attempt++;
                Thread.sleep(calculateBackoff(attempt));
            }
        }
    }

    /**
     * Applies retry logic to database operations.
     *
     * @param joinPoint the join point
     * @return method result
     * @throws Throwable if method execution fails after all retries
     */
    @Around("execution(* com.gogidix.UserManagement.EnterpriseTestService.infrastructure.persistence..*(..))")
    @Retryable(
        value = { org.springframework.dao.DataAccessException.class },
        maxAttempts = 2,
        backoff = @Backoff(delay = 500)
    )
    public Object retryDatabaseOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            log.trace("Executing database operation: {}.{}", className, methodName);
            return joinPoint.proceed();
        } catch (org.springframework.dao.DataAccessException e) {
            log.warn("Database operation failed, retrying: {}.{}", className, methodName);
            Thread.sleep(500);
            return joinPoint.proceed();
        }
    }

    /**
     * Calculates exponential backoff delay.
     *
     * @param attempt the attempt number
     * @return delay in milliseconds
     */
    private long calculateBackoff(int attempt) {
        return (long) (1000 * Math.pow(2, attempt - 1));
    }
}