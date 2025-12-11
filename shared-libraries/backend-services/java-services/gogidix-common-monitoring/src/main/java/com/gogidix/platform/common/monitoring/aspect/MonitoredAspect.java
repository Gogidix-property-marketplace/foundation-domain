package com.gogidix.platform.common.monitoring.aspect;

import com.gogidix.platform.common.monitoring.annotation.Monitored;
import com.gogidix.platform.common.monitoring.service.MetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for monitoring method execution with @Monitored annotation
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MonitoredAspect {

    private final MetricsService metricsService;
    private final AtomicInteger concurrentExecutions = new AtomicInteger(0);

    @Around("@annotation(monitored)")
    public Object monitorMethod(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = getMetricName(monitored, className, methodName);

        // Prepare tags
        String[] baseTags = prepareTags(monitored, className, methodName);

        // Record concurrent executions if enabled
        AtomicLong concurrentGauge = null;
        if (monitored.recordConcurrentExecutions()) {
            concurrentGauge = metricsService.registerGauge(
                "method.concurrent.executions",
                mergeTags(baseTags, "method", metricName)
            );
        }

        Timer.Sample timerSample = Timer.start();
        boolean success = false;

        try {
            // Increment concurrent executions
            if (monitored.recordConcurrentExecutions() && concurrentGauge != null) {
                concurrentGauge.incrementAndGet();
            }

            // Proceed with method execution
            Object result = joinPoint.proceed();
            success = true;

            // Record success metrics
            if (monitored.recordSuccessFailure()) {
                metricsService.incrementCounter(
                    "method.success.count",
                    mergeTags(baseTags, "method", metricName)
                );
            }

            log.debug("Method {} executed successfully", metricName);
            return result;

        } catch (Exception ex) {
            // Record failure metrics
            if (monitored.recordSuccessFailure()) {
                metricsService.incrementCounter(
                    "method.failure.count",
                    mergeTags(baseTags, "method", metricName, "error", ex.getClass().getSimpleName())
                );
            }

            log.error("Method {} failed with error: {}", metricName, ex.getMessage());
            throw ex;

        } finally {
            // Record execution time
            if (monitored.recordExecutionTime()) {
                Timer timer = metricsService.timerBuilder(metricName)
                        .description(monitored.description().isEmpty() ?
                            "Execution time for " + metricName : monitored.description())
                        .tags(mergeTags(baseTags, "method", metricName))
                        .publishPercentiles(monitored.percentiles())
                        .register(metricsService.getMeterRegistry());
                timerSample.stop(timer);
            }

            // Decrement concurrent executions
            if (monitored.recordConcurrentExecutions() && concurrentGauge != null) {
                concurrentGauge.decrementAndGet();
            }
        }
    }

    private String getMetricName(Monitored monitored, String className, String methodName) {
        if (!monitored.name().isEmpty()) {
            return monitored.name();
        }
        if (!monitored.suffix().isEmpty()) {
            return className + "." + methodName + "." + monitored.suffix();
        }
        return className + "." + methodName;
    }

    private String[] prepareTags(Monitored monitored, String className, String methodName) {
        String[] baseTags = {"class", className, "method", methodName};

        if (monitored.tags().length > 0) {
            return mergeTags(baseTags, monitored.tags());
        }

        return baseTags;
    }

    private String[] mergeTags(String[] existingTags, String... newTags) {
        String[] result = Arrays.copyOf(existingTags, existingTags.length + newTags.length);
        System.arraycopy(newTags, 0, result, existingTags.length, newTags.length);
        return result;
    }

    /**
     * Monitor class-level methods when @Monitored is applied to class
     */
    @Around("@within(monitored)")
    public Object monitorClass(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        // Check if method has its own @Monitored annotation
        try {
            Monitored methodMonitored = joinPoint.getSignature()
                    .getDeclaringType()
                    .getMethod(joinPoint.getSignature().getName(),
                               Arrays.stream(joinPoint.getSignature().getClass().getMethods())
                                     .map(m -> m.getParameterTypes())
                                     .findFirst()
                                     .orElse(new Class[0]))
                    .getAnnotation(Monitored.class);

            if (methodMonitored != null) {
                return joinPoint.proceed();
            }
        } catch (NoSuchMethodException ex) {
            // Method not found, proceed with class-level monitoring
        }

        return monitorMethod(joinPoint, monitored);
    }
}