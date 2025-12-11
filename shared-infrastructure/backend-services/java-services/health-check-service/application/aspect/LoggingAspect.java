package com.gogidix.infrastructure.health.application.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging method execution.
 * Provides comprehensive logging for method calls, execution time, and results.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Logs method execution for service layer.
     *
     * @param joinPoint the join point
     * @return method result
     * @throws Throwable if method execution fails
     */
    @Around("execution(* com.gogidix.UserManagement.EnterpriseTestService.application.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("Entering {}.{} with arguments: {}", className, methodName, args);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.debug("Exiting {}.{} with result: {} (Execution time: {}ms)",
                     className, methodName, result, executionTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("Exception in {}.{} (Execution time: {}ms): {}",
                     className, methodName, executionTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Logs method execution for repository layer.
     *
     * @param joinPoint the join point
     * @return method result
     * @throws Throwable if method execution fails
     */
    @Around("execution(* com.gogidix.UserManagement.EnterpriseTestService.infrastructure.persistence..*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.trace("Repository call: {}.{}", className, methodName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.trace("Repository completed: {}.{} in {}ms", className, methodName, executionTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("Repository error: {}.{} in {}ms: {}",
                     className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Logs method execution for controller layer.
     *
     * @param joinPoint the join point
     * @return method result
     * @throws Throwable if method execution fails
     */
    @Around("execution(* com.gogidix.UserManagement.EnterpriseTestService.web.controller..*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("API request: {}.{} with arguments: {}", className, methodName, args);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("API response: {}.{} completed in {}ms", className, methodName, executionTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.error("API error: {}.{} in {}ms: {}",
                     className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}