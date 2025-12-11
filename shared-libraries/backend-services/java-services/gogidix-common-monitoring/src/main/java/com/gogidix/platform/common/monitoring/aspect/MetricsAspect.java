package com.gogidix.platform.common.monitoring.aspect;

import com.gogidix.platform.common.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.*;
import java.util.Collection;

/**
 * Aspect for automatic metrics collection with AOP
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MetricsAspect {

    private final MonitoringService monitoringService;

    @Around("@annotation(timed)")
    public Object timedMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String metricName = timed.value().isEmpty()
            ? className + "." + methodName
            : timed.value();

        try {
            Object result = joinPoint.proceed();

            if (timed.recordSuccess()) {
                monitoringService.recordTimer(metricName + ".success",
                    java.time.Duration.ofNanos(System.nanoTime() - System.nanoTime()));
            }

            return result;
        } catch (Exception e) {
            if (timed.recordFailure()) {
                monitoringService.recordTimer(metricName + ".failure",
                    java.time.Duration.ofNanos(System.nanoTime() - System.nanoTime()));
                monitoringService.incrementCounter(metricName + ".error",
                    "exception", e.getClass().getSimpleName());
            }
            throw e;
        }
    }

    @Around("@annotation(counted)")
    public Object countedMethod(ProceedingJoinPoint joinPoint, Counted counted) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String metricName = counted.value().isEmpty()
            ? className + "." + methodName
            : counted.value();

        monitoringService.incrementCounter(metricName + ".invoked");

        try {
            Object result = joinPoint.proceed();
            monitoringService.incrementCounter(metricName + ".success");
            return result;
        } catch (Exception e) {
            monitoringService.incrementCounter(metricName + ".error",
                "exception", e.getClass().getSimpleName());
            throw e;
        }
    }

    @Around("@annotation(gauge)")
    public Object gaugeMethod(ProceedingJoinPoint joinPoint, Gauge gauge) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String metricName = gauge.value().isEmpty()
            ? className + "." + methodName
            : gauge.value();

        try {
            Object result = joinPoint.proceed();

            // Record result size if applicable
            if (result != null && result instanceof Collection) {
                monitoringService.registerGauge(metricName + ".size",
                    (Collection<?>) result, Collection::size);
            }

            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    @Around("@within(restController) || @annotation(restController)")
    public Object restControllerMethod(ProceedingJoinPoint joinPoint, RestController restController) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();

        // Extract HTTP method and path from annotations
        String httpMethod = extractHttpMethod(signature);
        String path = extractPath(signature, className, methodName);

        long startTime = System.nanoTime();

        try {
            Object result = joinPoint.proceed();

            long duration = System.nanoTime() - startTime;
            monitoringService.recordApiCall(httpMethod, path, 200,
                java.time.Duration.ofNanos(duration));

            return result;
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            monitoringService.recordApiCall(httpMethod, path, 500,
                java.time.Duration.ofNanos(duration));
            throw e;
        }
    }

    @Around("@annotation(monitored)")
    public Object monitoredMethod(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String businessMetric = monitored.businessMetric();
        String serviceName = monitored.serviceName().isEmpty()
            ? className
            : monitored.serviceName();

        long startTime = System.nanoTime();

        try {
            Object result = joinPoint.proceed();

            long duration = System.nanoTime() - startTime;

            if (!businessMetric.isEmpty()) {
                monitoringService.recordBusinessMetric(businessMetric, serviceName,
                    java.time.Duration.ofNanos(duration), true);
            } else {
                monitoringService.recordTimer("method.execution.time",
                    java.time.Duration.ofNanos(duration),
                    "class", className,
                    "method", methodName,
                    "success", "true");
            }

            return result;
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;

            if (!businessMetric.isEmpty()) {
                monitoringService.recordBusinessMetric(businessMetric, serviceName,
                    java.time.Duration.ofNanos(duration), false);
            } else {
                monitoringService.recordTimer("method.execution.time",
                    java.time.Duration.ofNanos(duration),
                    "class", className,
                    "method", methodName,
                    "success", "false");
            }

            throw e;
        }
    }

    private String extractHttpMethod(MethodSignature signature) {
        if (signature.getMethod().isAnnotationPresent(GetMapping.class)) {
            return "GET";
        } else if (signature.getMethod().isAnnotationPresent(PostMapping.class)) {
            return "POST";
        } else if (signature.getMethod().isAnnotationPresent(PutMapping.class)) {
            return "PUT";
        } else if (signature.getMethod().isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE";
        } else if (signature.getMethod().isAnnotationPresent(PatchMapping.class)) {
            return "PATCH";
        } else {
            return "UNKNOWN";
        }
    }

    private String extractPath(MethodSignature signature, String className, String methodName) {
        String path = "/" + className.toLowerCase().replace("controller", "");

        if (signature.getMethod().isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = signature.getMethod().getAnnotation(RequestMapping.class);
            if (mapping.path().length > 0) {
                path += mapping.path()[0];
            } else if (mapping.value().length > 0) {
                path += mapping.value()[0];
            }
        } else if (signature.getMethod().isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = signature.getMethod().getAnnotation(GetMapping.class);
            if (mapping.path().length > 0) {
                path += mapping.path()[0];
            } else if (mapping.value().length > 0) {
                path += mapping.value()[0];
            }
        } else if (signature.getMethod().isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = signature.getMethod().getAnnotation(PostMapping.class);
            if (mapping.path().length > 0) {
                path += mapping.path()[0];
            } else if (mapping.value().length > 0) {
                path += mapping.value()[0];
            }
        } else if (signature.getMethod().isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = signature.getMethod().getAnnotation(PutMapping.class);
            if (mapping.path().length > 0) {
                path += mapping.path()[0];
            } else if (mapping.value().length > 0) {
                path += mapping.value()[0];
            }
        } else if (signature.getMethod().isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = signature.getMethod().getAnnotation(DeleteMapping.class);
            if (mapping.path().length > 0) {
                path += mapping.path()[0];
            } else if (mapping.value().length > 0) {
                path += mapping.value()[0];
            }
        } else if (signature.getMethod().isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = signature.getMethod().getAnnotation(PatchMapping.class);
            if (mapping.path().length > 0) {
                path += mapping.path()[0];
            } else if (mapping.value().length > 0) {
                path += mapping.value()[0];
            }
        }

        return path;
    }

    /**
     * Annotation for timing method execution
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Timed {
        String value() default "";
        boolean recordSuccess() default true;
        boolean recordFailure() default true;
    }

    /**
     * Annotation for counting method invocations
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Counted {
        String value() default "";
    }

    /**
     * Annotation for gauging method results
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Gauge {
        String value() default "";
    }

    /**
     * Annotation for comprehensive method monitoring
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Monitored {
        String businessMetric() default "";
        String serviceName() default "";
    }
}