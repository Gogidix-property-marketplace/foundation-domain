package com.gogidix.platform.common.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Database configuration for Gogidix platform
 */
@Slf4j
@Configuration
@EnableJpaRepositories(basePackages = "com.gogidix.platform")
public class DatabaseConfiguration {

    /**
     * Primary HikariCP datasource configuration
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    @ConditionalOnProperty(name = "spring.datasource.hikari.enabled", havingValue = "true", matchIfMissing = true)
    public DataSource dataSource(DatabaseProperties properties) {
        log.info("Creating HikariCP datasource with properties: {}", properties);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());

        // Connection pool settings
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        setConnectionPoolSettings(config);

        // Performance settings
        config.setConnectionTimeout(properties.getConnectionTimeout());
        config.setIdleTimeout(properties.getIdleTimeout());
        config.setMaxLifetime(properties.getMaxLifetime());

        // Validation
        config.setConnectionTestQuery(properties.getConnectionTestQuery());
        config.setValidationTimeout(properties.getValidationTimeout());

        // Leak detection
        config.setLeakDetectionThreshold(properties.getLeakDetectionThreshold());

        // Pool name
        config.setPoolName(properties.getPoolName());

        return new HikariDataSource(config);
    }

    private void setConnectionPoolSettings(HikariConfig config) {
        // Set connection pool timeout to be longer than HikariCP defaults for cloud environments
        config.setPoolName("gogidix-hikari-pool");

        // Allow slow connections in production environments
        config.setConnectionTimeout(30000); // 30 seconds
        config.setValidationTimeout(5000); // 5 seconds
    }

    /**
     * JPA entity manager factory
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, JpaProperties jpaProperties) {

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.gogidix.platform");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        vendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        vendorAdapter.setShowSql(jpaProperties.isShowSql());

        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaProperties(getHibernateProperties(jpaProperties));

        return emf;
    }

    private java.util.Properties getHibernateProperties(JpaProperties jpaProperties) {
        java.util.Properties properties = new java.util.Properties();

        // DDL settings
        properties.setProperty("hibernate.hbm2ddl.auto", jpaProperties.getHibernateDdlAuto());

        // SQL dialect
        properties.setProperty("hibernate.dialect", jpaProperties.getDatabasePlatform());

        // Show SQL
        properties.setProperty("hibernate.show_sql", String.valueOf(jpaProperties.isShowSql()));
        properties.setProperty("hibernate.format_sql", String.valueOf(jpaProperties.isFormatSql()));
        properties.setProperty("hibernate.use_sql_comments", String.valueOf(jpaProperties.isUseSqlComments()));

        // Performance settings
        properties.setProperty("hibernate.jdbc.batch_size", String.valueOf(jpaProperties.getBatchSize()));
        properties.setProperty("hibernate.order_inserts", String.valueOf(jpaProperties.isOrderInserts()));
        properties.setProperty("hibernate.order_updates", String.valueOf(jpaProperties.isOrderUpdates()));
        properties.setProperty("hibernate.jdbc.batch_versioned_data", String.valueOf(jpaProperties.isBatchVersionedData()));
        properties.setProperty("hibernate.jdbc.fetch_size", String.valueOf(jpaProperties.getFetchSize()));

        // Statement cache
        properties.setProperty("hibernate.statement_cache_size", String.valueOf(jpaProperties.getStatementCacheSize()));
        properties.setProperty("hibernate.cache.use_second_level_cache", String.valueOf(jpaProperties.isUseSecondLevelCache()));
        properties.setProperty("hibernate.cache.use_query_cache", String.valueOf(jpaProperties.isUseQueryCache()));

        // Caching settings
        if (jpaProperties.getCacheRegionFactory() != null) {
            properties.setProperty("hibernate.cache.region.factory_class", jpaProperties.getCacheRegionFactory());
        }
        properties.setProperty("hibernate.cache.use_minimal_puts", String.valueOf(jpaProperties.isUseMinimalPuts()));
        properties.setProperty("hibernate.cache.use_structured_entries", String.valueOf(jpaProperties.isUseStructuredEntries()));

        // Statistics
        properties.setProperty("hibernate.generate_statistics", String.valueOf(jpaProperties.isGenerateStatistics()));

        // Connection settings
        properties.setProperty("hibernate.jdbc.non_contextual_creation", String.valueOf(jpaProperties.isNonContextualCreation()));
        properties.setProperty("hibernate.connection.provider_disables_autocommit", String.valueOf(jpaProperties.isConnectionProviderDisablesAutoCommit()));

        // Multi-tenancy
        if (jpaProperties.getMultiTenancyStrategy() != null) {
            properties.setProperty("hibernate.multi_tenancy", jpaProperties.getMultiTenancyStrategy());
        }

        return properties;
    }

    /**
     * Transaction manager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    /**
     * JDBC template for native SQL queries
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Database properties configuration
     */
    @ConfigurationProperties(prefix = "spring.datasource")
    public static class DatabaseProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private Integer maximumPoolSize = 20;
        private Integer minimumIdle = 5;
        private Long connectionTimeout = 30000L;
        private Long idleTimeout = 600000L;
        private Long maxLifetime = 1800000L;
        private String connectionTestQuery = "SELECT 1";
        private Long validationTimeout = 5000L;
        private Long leakDetectionThreshold = 0L;
        private String poolName = "gogidix-pool";

        // Getters
        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDriverClassName() { return driverClassName; }
        public Integer getMaximumPoolSize() { return maximumPoolSize; }
        public Integer getMinimumIdle() { return minimumIdle; }
        public Long getConnectionTimeout() { return connectionTimeout; }
        public Long getIdleTimeout() { return idleTimeout; }
        public Long getMaxLifetime() { return maxLifetime; }
        public String getConnectionTestQuery() { return connectionTestQuery; }
        public Long getValidationTimeout() { return validationTimeout; }
        public Long getLeakDetectionThreshold() { return leakDetectionThreshold; }
        public String getPoolName() { return poolName; }
    }

    /**
     * JPA properties configuration
     */
    @ConfigurationProperties(prefix = "spring.jpa")
    public static class JpaProperties {
        private String databasePlatform;
        private boolean generateDdl = false;
        private boolean showSql = false;
        private boolean formatSql = true;
        private boolean useSqlComments = true;
        private String hibernateDdlAuto = "none";
        private int batchSize = 20;
        private boolean orderInserts = true;
        private boolean orderUpdates = true;
        private boolean batchVersionedData = true;
        private int fetchSize = 100;
        private int statementCacheSize = 250;
        private boolean useSecondLevelCache = false;
        private boolean useQueryCache = true;
        private String cacheRegionFactory = null;
        private boolean useMinimalPuts = true;
        private boolean useStructuredEntries = true;
        private boolean generateStatistics = false;
        private boolean nonContextualCreation = true;
        private boolean connectionProviderDisablesAutoCommit = true;
        private String multiTenancyStrategy = null;

        // Getters
        public String getDatabasePlatform() { return databasePlatform; }
        public boolean isGenerateDdl() { return generateDdl; }
        public boolean isShowSql() { return showSql; }
        public boolean isFormatSql() { return formatSql; }
        public boolean isUseSqlComments() { return useSqlComments; }
        public String getHibernateDdlAuto() { return hibernateDdlAuto; }
        public int getBatchSize() { return batchSize; }
        public boolean isOrderInserts() { return orderInserts; }
        public boolean isOrderUpdates() { return orderUpdates; }
        public boolean isBatchVersionedData() { return batchVersionedData; }
        public int getFetchSize() { return fetchSize; }
        public int getStatementCacheSize() { return statementCacheSize; }
        public boolean isUseSecondLevelCache() { return useSecondLevelCache; }
        public boolean isUseQueryCache() { return useQueryCache; }
        public String getCacheRegionFactory() { return cacheRegionFactory; }
        public boolean isUseMinimalPuts() { return useMinimalPuts; }
        public boolean isUseStructuredEntries() { return useStructuredEntries; }
        public boolean isGenerateStatistics() { return generateStatistics; }
        public boolean isNonContextualCreation() { return nonContextualCreation; }
        public boolean isConnectionProviderDisablesAutoCommit() { return connectionProviderDisablesAutoCommit; }
        public String getMultiTenancyStrategy() { return multiTenancyStrategy; }
    }
}