package com.gogidix.infrastructure.cache.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-database configuration for EnterpriseTestService application.
 * Supports PostgreSQL, MySQL, and H2 databases.
 *
 * @author Gogidix Enterprise
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
public class DatabaseConfig {

    private final Environment env;

    /**
     * Primary datasource configuration.
     *
     * @return Primary DataSource bean
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        log.info("Configuring primary datasource: postgresql");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        String databaseType = env.getProperty("app.database.primary.type", "postgresql");
        String host = env.getProperty("app.database.primary.host", "localhost");
        String port = env.getProperty("app.database.primary.port", getDefaultPort(databaseType));
        String name = env.getProperty("app.database.primary.name", "EnterpriseTestService_EnterpriseTestService");
        String username = env.getProperty("app.database.primary.username");
        String password = env.getProperty("app.database.primary.password");

        String jdbcUrl = String.format("jdbc:%s://%s:%s/%s", databaseType, host, port, name);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(getDriverClassName(databaseType));

        // Connection pool settings
        dataSource.setConnectionProperties(getConnectionProperties());

        return dataSource;
    }

    /**
     * Read replica datasource for scaling read operations.
     *
     * @return Read replica DataSource bean
     */
    @Bean
    @ConditionalOnProperty(name = "app.database.read-replica.enabled", havingValue = "true")
    public DataSource readReplicaDataSource() {
        log.info("Configuring read replica datasource");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        String databaseType = env.getProperty("app.database.read-replica.type", "postgresql");
        String host = env.getProperty("app.database.read-replica.host");
        String port = env.getProperty("app.database.read-replica.port", getDefaultPort(databaseType));
        String name = env.getProperty("app.database.read-replica.name", "EnterpriseTestService_EnterpriseTestService");
        String username = env.getProperty("app.database.read-replica.username");
        String password = env.getProperty("app.database.read-replica.password");

        String jdbcUrl = String.format("jdbc:%s://%s:%s/%s", databaseType, host, port, name);
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(getDriverClassName(databaseType));

        return dataSource;
    }

    /**
     * JPA Entity Manager Factory configuration.
     *
     * @return LocalContainerEntityManagerFactoryBean
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(primaryDataSource());
        em.setPackagesToScan("com.gogidix.UserManagement.EnterpriseTestService.domain", "com.gogidix.UserManagement.EnterpriseTestService.infrastructure.persistence");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        // JPA properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto", "validate"));
        properties.put("hibernate.dialect", getDialect());
        properties.put("hibernate.show_sql", env.getProperty("spring.jpa.show-sql", "false"));
        properties.put("hibernate.format_sql", env.getProperty("spring.jpa.format-sql", "true"));
        properties.put("hibernate.use_sql_comments", env.getProperty("spring.jpa.properties.hibernate.use_sql_comments", "true"));
        properties.put("hibernate.jdbc.batch_size", env.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "20"));
        properties.put("hibernate.order_inserts", env.getProperty("spring.jpa.properties.hibernate.order_inserts", "true"));
        properties.put("hibernate.order_updates", env.getProperty("spring.jpa.properties.hibernate.order_updates", "true"));
        properties.put("hibernate.jdbc.fetch_size", env.getProperty("spring.jpa.properties.hibernate.jdbc.fetch_size", "100"));

        // Connection pool settings
        properties.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        properties.put("hibernate.hikari.maximum-pool-size", env.getProperty("spring.datasource.hikari.maximum-pool-size", "20"));
        properties.put("hibernate.hikari.minimum-idle", env.getProperty("spring.datasource.hikari.minimum-idle", "5"));
        properties.put("hibernate.hikari.idle-timeout", env.getProperty("spring.datasource.hikari.idle-timeout", "30000"));
        properties.put("hibernate.hikari.max-lifetime", env.getProperty("spring.datasource.hikari.max-lifetime", "1800000"));
        properties.put("hibernate.hikari.connection-timeout", env.getProperty("spring.datasource.hikari.connection-timeout", "30000"));

        em.setJpaProperties(properties);

        return em;
    }

    /**
     * Transaction manager configuration.
     *
     * @return PlatformTransactionManager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    private String getDefaultPort(String databaseType) {
        switch (databaseType.toLowerCase()) {
            case "postgresql":
                return "5432";
            case "mysql":
                return "3306";
            case "h2":
                return "9092";
            default:
                return "5432";
        }
    }

    private String getDriverClassName(String databaseType) {
        switch (databaseType.toLowerCase()) {
            case "postgresql":
                return "org.postgresql.Driver";
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "h2":
                return "org.h2.Driver";
            default:
                return "org.postgresql.Driver";
        }
    }

    private String getDialect() {
        String databaseType = env.getProperty("app.database.primary.type", "postgresql");
        switch (databaseType.toLowerCase()) {
            case "postgresql":
                return "org.hibernate.dialect.PostgreSQLDialect";
            case "mysql":
                return "org.hibernate.dialect.MySQL8Dialect";
            case "h2":
                return "org.hibernate.dialect.H2Dialect";
            default:
                return "org.hibernate.dialect.PostgreSQLDialect";
        }
    }

    private String getConnectionProperties() {
        StringBuilder properties = new StringBuilder();

        // SSL configuration
        String sslMode = env.getProperty("app.database.primary.ssl.mode", "prefer");
        properties.append("ssl=").append(sslMode).append(";");

        // Connection timeout
        String connectTimeout = env.getProperty("app.database.primary.connect-timeout", "10");
        properties.append("connectTimeout=").append(connectTimeout).append(";");

        // Socket timeout
        String socketTimeout = env.getProperty("app.database.primary.socket-timeout", "30");
        properties.append("socketTimeout=").append(socketTimeout).append(";");

        // Preparation threshold
        properties.append("prepareThreshold=0;");

        return properties.toString();
    }
}