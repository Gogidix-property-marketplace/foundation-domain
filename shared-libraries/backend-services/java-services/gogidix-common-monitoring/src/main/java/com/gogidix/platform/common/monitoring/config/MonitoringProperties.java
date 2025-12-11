package com.gogidix.platform.common.monitoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gogidix Common Monitoring
 */
@Data
@ConfigurationProperties(prefix = "gogidix.monitoring")
public class MonitoringProperties {

    /**
     * Enable/disable monitoring
     */
    private boolean enabled = true;

    /**
     * Application name for metrics tagging
     */
    private String applicationName = "gogidix-application";

    /**
     * Enable JVM metrics
     */
    private boolean enableJvmMetrics = true;

    /**
     * Enable system metrics
     */
    private boolean enableSystemMetrics = true;

    /**
     * Enable logging metrics
     */
    private boolean enableLoggingMetrics = true;

    /**
     * Enable API metrics
     */
    private boolean enableApiMetrics = true;

    /**
     * Enable business metrics
     */
    private boolean enableBusinessMetrics = true;

    /**
     * Enable database metrics
     */
    private boolean enableDatabaseMetrics = true;

    /**
     * Enable cache metrics
     */
    private boolean enableCacheMetrics = true;

    /**
     * Health indicator configuration
     */
    private HealthIndicator healthIndicator = new HealthIndicator();

    /**
     * Metrics publication configuration
     */
    private MetricsPublication metricsPublication = new MetricsPublication();

    /**
     * Prometheus configuration
     */
    private Prometheus prometheus = new Prometheus();

    /**
     * InfluxDB configuration
     */
    private Influx influx = new Influx();

    /**
     * Graphite configuration
     */
    private Graphite graphite = new Graphite();

    /**
     * StatsD configuration
     */
    private Statsd statsd = new Statsd();

    @Data
    public static class HealthIndicator {
        /**
         * Enable health indicator
         */
        private boolean enabled = true;

        /**
         * Default health status
         */
        private String defaultStatus = "UP";

        /**
         * Health check interval in seconds
         */
        private int checkInterval = 60;
    }

    @Data
    public static class MetricsPublication {
        /**
         * Enable metrics publication
         */
        private boolean enabled = true;

        /**
         * Publication interval in seconds
         */
        private int interval = 30;

        /**
         * Buffer size for metrics
         */
        private int bufferSize = 1000;

        /**
         * Enable metrics compression
         */
        private boolean compressionEnabled = true;
    }

    @Data
    public static class Prometheus {
        /**
         * Enable Prometheus metrics
         */
        private boolean enabled = true;

        /**
         * Prometheus endpoint
         */
        private String endpoint = "/actuator/prometheus";

        /**
         * Enable Prometheus histograms
         */
        private boolean histograms = true;

        /**
         * Histogram percentiles
         */
        private double[] percentiles = {0.5, 0.75, 0.95, 0.99};
    }

    @Data
    public static class Influx {
        /**
         * Enable InfluxDB metrics
         */
        private boolean enabled = false;

        /**
         * InfluxDB URI
         */
        private String uri = "http://localhost:8086";

        /**
         * Database name
         */
        private String database = "gogidix_metrics";

        /**
         * Username
         */
        private String username = "";

        /**
         * Password
         */
        private String password = "";

        /**
         * Retention policy
         */
        private String retentionPolicy = "autogen";
    }

    @Data
    public static class Graphite {
        /**
         * Enable Graphite metrics
         */
        private boolean enabled = false;

        /**
         * Graphite host
         */
        private String host = "localhost";

        /**
         * Graphite port
         */
        private int port = 2004;

        /**
         * Graphite protocol (tcp or udp)
         */
        private String protocol = "tcp";

        /**
         * Metrics prefix
         */
        private String prefix = "gogidix";
    }

    @Data
    public static class Statsd {
        /**
         * Enable StatsD metrics
         */
        private boolean enabled = false;

        /**
         * StatsD host
         */
        private String host = "localhost";

        /**
         * StatsD port
         */
        private int port = 8125;

        /**
         * Metrics prefix
         */
        private String prefix = "gogidix";

        /**
         * Enable flavor (datadog, etsy, telegraf)
         */
        private String flavor = "etsy";
    }
}