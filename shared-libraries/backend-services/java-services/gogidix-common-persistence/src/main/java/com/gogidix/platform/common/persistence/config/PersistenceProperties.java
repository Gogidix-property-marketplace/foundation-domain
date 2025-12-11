package com.gogidix.platform.common.persistence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Gogidix Common Persistence
 */
@Data
@ConfigurationProperties(prefix = "gogidix.persistence")
public class PersistenceProperties {

    /**
     * Enable/disable persistence features
     */
    private boolean enabled = true;

    /**
     * Database configuration
     */
    private Database database = new Database();

    /**
     * JPA configuration
     */
    private Jpa jpa = new Jpa();

    /**
     * Flyway migration configuration
     */
    private Flyway flyway = new Flyway();

    /**
     * Liquibase migration configuration
     */
    private Liquibase liquibase = new Liquibase();

    /**
     * Auditing configuration
     */
    private Auditing auditing = new Auditing();

    /**
     * Multi-tenancy configuration
     */
    private MultiTenancy multiTenancy = new MultiTenancy();

    @Data
    public static class Database {
        /**
         * Database platform
         */
        private String platform = "postgresql";

        /**
         * Database name
         */
        private String name = "gogidix";

        /**
         * Database host
         */
        private String host = "localhost";

        /**
         * Database port
         */
        private int port = 5432;

        /**
         * Database username
         */
        private String username = "gogidix";

        /**
         * Database password
         */
        private String password = "";

        /**
         * Connection pool configuration
         */
        private ConnectionPool connectionPool = new ConnectionPool();

        /**
         * SSL configuration
         */
        private boolean sslEnabled = false;

        /**
         * SSL mode
         */
        private String sslMode = "require";

        /**
         * SSL certificate path
         */
        private String sslCert = "";

        /**
         * SSL key path
         */
        private String sslKey = "";

        /**
         * SSL root certificate path
         */
        private String sslRootCert = "";
    }

    @Data
    public static class ConnectionPool {
        /**
         * Minimum pool size
         */
        private int minimumIdle = 5;

        /**
         * Maximum pool size
         */
        private int maximumPoolSize = 20;

        /**
         * Connection timeout in milliseconds
         */
        private long connectionTimeout = 30000;

        /**
         * Idle timeout in milliseconds
         */
        private long idleTimeout = 600000;

        /**
         * Max lifetime in milliseconds
         */
        private long maxLifetime = 1800000;

        /**
         * Leak detection threshold in milliseconds
         */
        private long leakDetectionThreshold = 0;

        /**
         * Validation timeout in milliseconds
         */
        private long validationTimeout = 5000;

        /**
         * Enable connection testing
         */
        private boolean connectionTest = true;

        /**
         * Connection test query
         */
        private String connectionTestQuery = "SELECT 1";
    }

    @Data
    public static class Jpa {
        /**
         * DDL auto generation mode
         */
        private String ddlAuto = "validate";

        /**
         * Show SQL
         */
        private boolean showSql = false;

        /**
         * Format SQL
         */
        private boolean formatSql = false;

        /**
         * Use SQL comments
         */
        private boolean useSqlComments = false;

        /**
         * Generate statistics
         */
        private boolean generateStatistics = false;

        /**
         * JDBC batch size
         */
        private int jdbcBatchSize = 25;

        /**
         * Order inserts
         */
        private boolean orderInserts = true;

        /**
         * Order updates
         */
        private boolean orderUpdates = true;

        /**
         * Query cache
         */
        private boolean queryCache = true;

        /**
         * Second level cache
         */
        private boolean secondLevelCache = false;

        /**
         * Cache region factory
         */
        private String cacheRegionFactory = "org.hibernate.cache.ehcache.internal.EhcacheRegionFactory";

        /**
         * Batch versioned data
         */
        private boolean jdbcBatchVersionedData = true;
    }

    @Data
    public static class Flyway {
        /**
         * Enable Flyway migrations
         */
        private boolean enabled = false;

        /**
         * Migration locations
         */
        private String[] locations = {"classpath:db/migration"};

        /**
         * Baseline on migrate
         */
        private boolean baselineOnMigrate = false;

        /**
         * Baseline version
         */
        private String baselineVersion = "1";

        /**
         * Validate on migrate
         */
        private boolean validateOnMigrate = true;

        /**
         * Clean disabled
         */
        private boolean cleanDisabled = true;

        /**
         * Clean on validation error
         */
        private boolean cleanOnValidationError = false;

        /**
         * Out of order
         */
        private boolean outOfOrder = false;
    }

    @Data
    public static class Liquibase {
        /**
         * Enable Liquibase migrations
         */
        private boolean enabled = false;

        /**
         * Change log location
         */
        private String changeLog = "classpath:db/changelog/db.changelog-master.xml";

        /**
         * Drop first
         */
        private boolean dropFirst = false;

        /**
         * Contexts
         */
        private String contexts = "";

        /**
         * Labels
         */
        private String labels = "";

        /**
         * Default schema name
         */
        private String defaultSchemaName = "public";

        /**
         * Liquibase schema name
         */
        private String liquibaseSchemaName = "public";
    }

    @Data
    public static class Auditing {
        /**
         * Enable JPA auditing
         */
        private boolean enabled = true;

        /**
         * Auditor provider implementation
         */
        private String auditorProvider = "default";

        /**
         * Set modify on creation
         */
        private boolean setModifyOnCreation = true;

        /**
         * DateTime provider
         */
        private String dateTimeProvider = "instant";
    }

    @Data
    public static class MultiTenancy {
        /**
         * Enable multi-tenancy
         */
        private boolean enabled = false;

        /**
         * Tenant strategy (schema, database, discriminator)
         */
        private String strategy = "schema";

        /**
         * Default tenant schema
         */
        private String defaultSchema = "public";

        /**
         * Tenant schema prefix
         */
        private String tenantSchemaPrefix = "tenant_";

        /**
         * Tenant identifier resolver
         */
        private String tenantIdentifierResolver = "default";
    }
}