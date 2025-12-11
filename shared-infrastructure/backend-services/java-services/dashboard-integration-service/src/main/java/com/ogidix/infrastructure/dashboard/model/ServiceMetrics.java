package com.ogidix.infrastructure.dashboard.model;

import java.time.LocalDateTime;

/**
 * Service Metrics for Dashboard Integration
 * Represents individual service performance metrics
 */
public class ServiceMetrics {

    private String serviceName;
    private String instanceId;
    private String status; // UP, DOWN, DEGRADED
    private double cpuUsage;
    private double memoryUsage;
    private long activeConnections;
    private long totalRequests;
    private long errorCount;
    private double averageResponseTime;
    private double p95ResponseTime;
    private double p99ResponseTime;
    private String lastError;
    private LocalDateTime lastUpdated;
    private LocalDateTime lastHealthCheck;

    public ServiceMetrics() {
        this.lastUpdated = LocalDateTime.now();
        this.lastHealthCheck = LocalDateTime.now();
    }

    public ServiceMetrics(String serviceName, String instanceId, String status) {
        this();
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.status = status;
    }

    // Getters and Setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public long getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(long activeConnections) {
        this.activeConnections = activeConnections;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public double getP95ResponseTime() {
        return p95ResponseTime;
    }

    public void setP95ResponseTime(double p95ResponseTime) {
        this.p95ResponseTime = p95ResponseTime;
    }

    public double getP99ResponseTime() {
        return p99ResponseTime;
    }

    public void setP99ResponseTime(double p99ResponseTime) {
        this.p99ResponseTime = p99ResponseTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }

    @Override
    public String toString() {
        return "ServiceMetrics{" +
                "serviceName='" + serviceName + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", status='" + status + '\'' +
                ", cpuUsage=" + cpuUsage +
                ", memoryUsage=" + memoryUsage +
                ", activeConnections=" + activeConnections +
                ", totalRequests=" + totalRequests +
                ", errorCount=" + errorCount +
                ", averageResponseTime=" + averageResponseTime +
                ", p95ResponseTime=" + p95ResponseTime +
                ", p99ResponseTime=" + p99ResponseTime +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}