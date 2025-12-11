package com.gogidix.platform.common.cache.aspect;

import com.gogidix.platform.common.cache.annotation.CacheConfig;
import com.gogidix.platform.common.cache.service.GogidixCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Aspect for handling cache operations with @CacheConfig annotation
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {

    private final GogidixCacheService cacheService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(cacheConfig)")
    public Object handleCache(ProceedingJoinPoint joinPoint, CacheConfig cacheConfig) throws Throwable {
        String cacheName = getCacheName(cacheConfig, joinPoint);
        String cacheKey = generateCacheKey(cacheConfig, joinPoint);

        // Check if caching is disabled
        if (cacheConfig.ttl() == -1) {
            log.debug("Caching disabled for method: {}", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        // Try to get from cache first
        Object cachedValue = cacheService.get(cacheName, cacheKey, Object.class);
        if (cachedValue != null) {
            log.debug("Cache hit for key '{}' in cache '{}'", cacheKey, cacheName);
            return cachedValue;
        }

        // Cache miss, execute method
        Object result = joinPoint.proceed();

        // Check unless condition
        if (shouldSkipCaching(cacheConfig.unless(), result, joinPoint)) {
            log.debug("Skipping cache due to unless condition for key '{}' in cache '{}'", cacheKey, cacheName);
            return result;
        }

        // Check condition
        if (!shouldCache(cacheConfig.condition(), result, joinPoint)) {
            log.debug("Skipping cache due to condition for key '{}' in cache '{}'", cacheKey, cacheName);
            return result;
        }

        // Check if null values should be cached
        if (result == null && !cacheConfig.cacheNull()) {
            log.debug("Not caching null value for key '{}' in cache '{}'", cacheKey, cacheName);
            return result;
        }

        // Put result in cache
        Duration ttl = cacheConfig.ttl() > 0 ?
            Duration.of(cacheConfig.ttl(), cacheConfig.timeUnit().toChronoUnit()) :
            Duration.ofHours(1); // Default TTL

        cacheService.put(cacheName, cacheKey, result, ttl);
        log.debug("Cached result for key '{}' in cache '{}' with TTL: {}", cacheKey, cacheName, ttl);

        return result;
    }

    private String getCacheName(CacheConfig cacheConfig, ProceedingJoinPoint joinPoint) {
        // Use explicit cache name if provided
        if (StringUtils.hasText(cacheConfig.cacheName())) {
            return cacheConfig.cacheName();
        }

        // Derive cache name from class name
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1) + "Cache";
    }

    private String generateCacheKey(CacheConfig cacheConfig, ProceedingJoinPoint joinPoint) {
        // Use explicit key if provided
        if (StringUtils.hasText(cacheConfig.key())) {
            return evaluateSpelExpression(cacheConfig.key(), joinPoint);
        }

        // Generate default key based on method signature and parameters
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method.getName());

        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            keyBuilder.append(":");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    keyBuilder.append(",");
                }
                keyBuilder.append(args[i] != null ? args[i].toString() : "null");
            }
        }

        return keyBuilder.toString();
    }

    private String evaluateSpelExpression(String expression, ProceedingJoinPoint joinPoint) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Add method parameters as variables
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }

            // Add target object
            context.setRootObject(joinPoint.getTarget());

            // Add method name
            context.setVariable("methodName", signature.getMethod().getName());

            Expression expr = expressionParser.parseExpression(expression);
            Object result = expr.getValue(context);

            return result != null ? result.toString() : "null";
        } catch (Exception e) {
            log.error("Error evaluating SpEL expression: {}", expression, e);
            return "spel-error-" + expression.hashCode();
        }
    }

    private boolean shouldCache(String condition, Object result, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(condition)) {
            return true;
        }

        try {
            String evaluatedCondition = evaluateSpelExpression(condition, joinPoint);
            return Boolean.parseBoolean(evaluatedCondition);
        } catch (Exception e) {
            log.error("Error evaluating cache condition: {}", condition, e);
            return true; // Default to caching if condition evaluation fails
        }
    }

    private boolean shouldSkipCaching(String unless, Object result, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(unless)) {
            return false;
        }

        try {
            String evaluatedUnless = evaluateSpelExpression(unless, joinPoint);
            return Boolean.parseBoolean(evaluatedUnless);
        } catch (Exception e) {
            log.error("Error evaluating cache unless condition: {}", unless, e);
            return false; // Default to caching if unless evaluation fails
        }
    }

    /**
     * Handle cache eviction
     */
    @Around("@annotation(com.gogidix.platform.common.cache.annotation.CacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        com.gogidix.platform.common.cache.annotation.CacheEvict cacheEvict =
            AnnotationUtils.findAnnotation(method, com.gogidix.platform.common.cache.annotation.CacheEvict.class);

        if (cacheEvict == null) {
            return joinPoint.proceed();
        }

        String cacheName = StringUtils.hasText(cacheEvict.cacheName()) ?
            cacheEvict.cacheName() :
            getDefaultCacheName(joinPoint);

        if (cacheEvict.allEntries()) {
            // Clear entire cache
            cacheService.clear(cacheName);
            log.debug("Cleared entire cache: {}", cacheName);
        } else {
            // Evict specific key
            String cacheKey = StringUtils.hasText(cacheEvict.key()) ?
                evaluateSpelExpression(cacheEvict.key(), joinPoint) :
                generateDefaultCacheKey(joinPoint);

            cacheService.evict(cacheName, cacheKey);
            log.debug("Evicted key '{}' from cache '{}'", cacheKey, cacheName);
        }

        // Execute before or after method based on configuration
        if (cacheEvict.beforeInvocation()) {
            Object result = joinPoint.proceed();
            return result;
        } else {
            Object result = joinPoint.proceed();
            return result;
        }
    }

    /**
     * Handle cache put operation
     */
    @Around("@annotation(com.gogidix.platform.common.cache.annotation.CachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        Method putMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        com.gogidix.platform.common.cache.annotation.CachePut cachePut =
            AnnotationUtils.findAnnotation(putMethod, com.gogidix.platform.common.cache.annotation.CachePut.class);

        if (cachePut == null) {
            return joinPoint.proceed();
        }

        // Execute method
        Object result = joinPoint.proceed();

        String cacheName = StringUtils.hasText(cachePut.cacheName()) ?
            cachePut.cacheName() :
            getDefaultCacheName(joinPoint);

        String cacheKey = StringUtils.hasText(cachePut.key()) ?
            evaluateSpelExpression(cachePut.key(), joinPoint) :
            generateDefaultCacheKey(joinPoint);

        // Put result in cache
        cacheService.put(cacheName, cacheKey, result);
        log.debug("Put result for key '{}' in cache '{}'", cacheKey, cacheName);

        return result;
    }

    private String getDefaultCacheName(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1) + "Cache";
    }

    private String generateDefaultCacheKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getName();
    }
}