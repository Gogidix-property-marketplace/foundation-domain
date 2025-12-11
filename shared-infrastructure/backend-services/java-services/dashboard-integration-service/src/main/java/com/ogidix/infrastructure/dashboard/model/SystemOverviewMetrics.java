package com.ogidix.infrastructure.dashboard.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * System Overview Metrics for Dashboard Integration
 * Represents overall system health and performance metrics
 */
public class SystemOverviewMetrics {

    private String timestamp;
    private int totalServices;
    private int healthyServices;
    private int unhealthyServices;
    private double systemCpuUsage;
    private double systemMemoryUsage;
    private long totalRequests;
    private double averageResponseTime;
    private Map<String, Object> customMetrics;
    private LocalDateTime lastUpdated;

    public SystemOverviewMetrics() {
        this.lastUpdated = LocalDateTime.now();
    }

    public SystemOverviewMetrics(String timestamp, int totalServices, int healthyServices,
                                int unhealthyServices, double systemCpuUsage, double systemMemoryUsage,
                                long totalRequests, double averageResponseTime) {
        this.timestamp = timestamp;
        this.totalServices = totalServices;
        this.healthyServices = healthyServices;
        this.unhealthyServices = unhealthyServices;
        this.systemCpuUsage = systemCpuUsage;
        this.systemMemoryUsage = systemMemoryUsage;
        this.totalRequests = totalRequests;
        this.averageResponseTime = averageResponseTime;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getTotalServices() {
        return totalServices;
    }

    public void setTotalServices(int totalServices) {
        this.totalServices = totalServices;
    }

    public int getHealthyServices() {
        return healthyServices;
    }

    public void setHealthyServices(int healthyServices) {
        this.healthyServices = healthyServices;
    }

    public int getUnhealthyServices() {
        return unhealthyServices;
    }

    public void setUnhealthyServices(int unhealthyServices) {
        this.unhealthyServices = unhealthyServices;
    }

    public double getSystemCpuUsage() {
        return systemCpuUsage;
    }

    public void setSystemCpuUsage(double systemCpuUsage) {
        this.systemCpuUsage = systemCpuUsage;
    }

    public double getSystemMemoryUsage() {
        return systemMemoryUsage;
    }

    public void setSystemMemoryUsage(double systemMemoryUsage) {
        this.systemMemoryUsage = systemMemoryUsage;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public Map<String, Object> getCustomMetrics() {
        return customMetrics;
    }

    public void setCustomMetrics(Map<String, Object> customMetrics) {
        this.customMetrics = customMetrics;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "SystemOverviewMetrics{" +
                "timestamp='" + timestamp + '\'' +
                ", totalServices=" + totalServices +
                ", healthyServices=" + healthyServices +
                ", unhealthyServices=" + unhealthyServices +
                ", systemCpuUsage=" + systemCpuUsage +
                ", systemMemoryUsage=" + systemMemoryUsage +
                ", totalRequests=" + totalRequests +
                ", averageResponseTime=" + averageResponseTime +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}