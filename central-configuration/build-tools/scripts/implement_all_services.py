#!/usr/bin/env python3

import os
import shutil

base_path = r"C:\Users\HP\Desktop\Gogidix-property-marketplace\Gogidix-Domain\foundation-domain\central-configuration\backend-services\java-services"

# Services configuration
services = {
    "SecretsManagementService": {
        "port": 8890,
        "package": "com.gogidix.foundation.security",
        "path": "secrets-management",
        "db_name": "secrets_management_db"
    },
    "SecretsRotationService": {
        "port": 8891,
        "package": "com.gogidix.foundation.security",
        "path": "secrets-rotation",
        "db_name": "secrets_rotation_db"
    },
    "FeatureFlagsService": {
        "port": 8892,
        "package": "com.gogidix.foundation.config",
        "path": "feature-flags",
        "db_name": "feature_flags_db"
    },
    "RateLimitingService": {
        "port": 8893,
        "package": "com.gogidix.foundation.config",
        "path": "rate-limiting",
        "db_name": "rate_limiting_db"
    },
    "AuditLoggingConfigService": {
        "port": 8894,
        "package": "com.gogidix.foundation.audit",
        "path": "audit-logging",
        "db_name": "audit_logging_db"
    },
    "BackupConfigService": {
        "port": 8895,
        "package": "com.gogidix.foundation.backup",
        "path": "backup-config",
        "db_name": "backup_config_db"
    },
    "DisasterRecoveryConfigService": {
        "port": 8896,
        "package": "com.gogidix.foundation.disaster",
        "path": "disaster-recovery",
        "db_name": "disaster_recovery_db"
    },
    "EnvironmentVarsService": {
        "port": 8897,
        "package": "com.gogidix.foundation.config",
        "path": "environment-vars",
        "db_name": "environment_vars_db"
    },
    "PolicyManagementService": {
        "port": 8898,
        "package": "com.gogidix.foundation.policy",
        "path": "policy-management",
        "db_name": "policy_management_db"
    }
}

def create_directory_structure(service_name, service_config):
    """Create directory structure for a service"""
    service_path = os.path.join(base_path, service_name, service_name)
    src_path = os.path.join(service_path, "src", "main", "java", *service_config["package"].split("."))

    directories = [
        os.path.join(src_path, "controller"),
        os.path.join(src_path, "entity"),
        os.path.join(src_path, "repository"),
        os.path.join(src_path, "dto"),
        os.path.join(src_path, "service"),
        os.path.join(src_path, "enums")
    ]

    for directory in directories:
        os.makedirs(directory, exist_ok=True)

    # Create resources directories
    resources_dirs = [
        os.path.join(service_path, "src", "main", "resources", "db", "migration"),
        os.path.join(service_path, "src", "test", "java", *service_config["package"].split("."), "service")
    ]

    for directory in resources_dirs:
        os.makedirs(directory, exist_ok=True)

def update_application_yml(service_name, service_config):
    """Update application.yml for a service"""
    app_yml_path = os.path.join(base_path, service_name, service_name, "src", "main", "resources", "application.yml")

    yml_content = f"""server:
  port: {service_config['port']}
  http2:
    enabled: true
  compression:
    enabled: true
    mime-types: text/html, text/xml, text/plain, text/css, application/javascript, application/json
  servlet:
    context-path: /{service_config['path']}
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    accept-count: 100
    connection-timeout: 20000
    max-connections: 8192

spring:
  application:
    name: {service_name.lower()}
  profiles:
    active: dev

  # Security Configuration
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${{OAUTH2_ISSUER_URI:http://localhost:8080/auth/realms/gogidix}}

  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/{service_config['db_name']}
    username: ${{DB_USERNAME:postgres}}
    password: ${{DB_PASSWORD:password}}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      leak-detection-threshold: 60000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: false
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  # Flyway Database Migration
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true

  # Cache Configuration
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m

# Service Discovery
eureka:
  client:
    service-url:
      defaultZone: ${{EUREKA_SERVER:http://localhost:8761/eureka/}}
    register-with-eureka: true
    fetch-registry: true
    healthcheck:
      enabled: true
  instance:
    hostname: ${{HOSTNAME:localhost}}
    prefer-ip-address: true

# Monitoring and Metrics
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,configprops,env,loggers
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      show-components: always
  metrics:
    export:
      prometheus:
        enabled: true

# OpenAPI Documentation
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method

# Resilience4j Configuration
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
  ratelimiter:
    configs:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s

# Logging Configuration
logging:
  level:
    {service_config['package']}: INFO
    org.springframework.security: WARN
  pattern:
    console: "%d{{yyyy-MM-dd HH:mm:ss}} [%thread] %-5level %logger{{36}} - %msg%n"
  file:
    name: logs/{service_name}.log
    max-size: 100MB
    max-history: 30

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:{service_name.lower()}db
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
    show-sql: true
  flyway:
    enabled: false

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${{PROD_DB_URL:jdbc:postgresql://prod-db:5432/{service_config['db_name']}}}
    username: ${{PROD_DB_USERNAME:prod_user}}
    password: ${{PROD_DB_PASSWORD:}}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
"""

    with open(app_yml_path, 'w') as f:
        f.write(yml_content)

def update_pom_dependencies(service_name):
    """Update pom.xml to add Flyway dependency"""
    pom_path = os.path.join(base_path, service_name, service_name, "pom.xml")

    # Read the pom.xml
    with open(pom_path, 'r') as f:
        pom_content = f.read()

    # Add Flyway dependency after validation
    if '<artifactId>flyway-core</artifactId>' not in pom_content:
        # Find the position to insert Flyway dependency
        insert_pos = pom_content.find('</dependency>') + len('</dependency>')

        flyway_dep = """

        <!-- Database Migration -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>"""

        pom_content = pom_content[:insert_pos] + flyway_dep + pom_content[insert_pos:]

    # Update application name in pom.xml if needed
    pom_content = pom_content.replace(
        '<artifactId>' + service_name + '</artifactId>',
        '<artifactId>' + service_name.replace('Service', '') + '</artifactId>'
    )

    with open(pom_path, 'w') as f:
        f.write(pom_content)

print("Starting implementation of all services...")

# Implement each service
for service_name, config in services.items():
    print(f"\n=== Implementing {service_name} ===")

    # Create directory structure
    create_directory_structure(service_name, config)

    # Update application.yml
    update_application_yml(service_name, config)

    # Update pom.xml
    update_pom_dependencies(service_name)

    print(f"[OK] {service_name} structure created")

print("\n=== All services structure created! ===")
print("Next steps:")
print("1. Implement entities for each service")
print("2. Implement repositories")
print("3. Implement services")
print("4. Implement controllers")
print("5. Create database migrations")
print("6. Add unit tests")