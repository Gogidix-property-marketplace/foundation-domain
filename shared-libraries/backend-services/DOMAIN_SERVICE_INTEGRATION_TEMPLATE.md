# ✨ GOGIDIX PLATFORM - DOMAIN SERVICE INTEGRATION TEMPLATE ✨
# Enterprise Integration Guide for Shared Libraries
# Architectural Excellence: Google Cloud + Netflix Standards

# =====================================================
# DOMAIN SERVICE POM TEMPLATE WITH SHARED LIBRARIES INTEGRATION
# =====================================================
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- GOGIDIX SHARED LIBRARIES PARENT -->
    <parent>
        <groupId>com.gogidix.platform</groupId>
        <artifactId>gogidix-shared-libraries</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../../../foundation-domain/shared-libraries/backend-services/gogidix-shared-libraries/pom.xml</relativePath>
    </parent>

    <!-- SERVICE IDENTIFICATION -->
    <artifactId>billing-invoicing-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Gogidix Billing Invoicing Service</name>
    <description>Enterprise billing and invoicing management microservice with DDD architecture</description>

    <!-- ORGANIZATION -->
    <organization>
        <name>Gogidix Platform</name>
        <url>https://gogidix.com</url>
    </organization>

    <!-- GLOBAL PROPERTIES -->
    <properties>
        <!-- Service Configuration -->
        <service.name>billing-invoicing-service</service.name>
        <service.group>financial-management</service.group>
        <service.domain>billing</service.domain>

        <!-- Base Package for Scanning -->
        <base.package>com.gogidix.platform.billing</base.package>

        <!-- Application Entry Point -->
        <main.class>com.gogidix.platform.billing.BillingInvoicingApplication</main.class>

        <!-- Database Configuration -->
        <db.name>billing_invoicing</db.name>
        <flyway.locations>classpath:db/migration/billing</flyway.locations>

        <!-- Docker Configuration -->
        <docker.port>8084</docker.port>
        <docker.host.port>8084</docker.host.port>
    </properties>

    <!-- DEPENDENCIES -->
    <dependencies>
        <!-- ===================================================== -->
        <!-- GOGIDIX SHARED LIBRARIES -->
        <!-- ===================================================== -->

        <!-- Core Library - Response Framework, Validation, Utilities -->
        <dependency>
            <groupId>com.gogidix.platform</groupId>
            <artifactId>gogidix-common-core</artifactId>
        </dependency>

        <!-- Security Library - JWT, RBAC, OAuth2 -->
        <dependency>
            <groupId>com.gogidix.platform</groupId>
            <artifactId>gogidix-common-security</artifactId>
        </dependency>

        <!-- Messaging Library - Kafka, Event Bus -->
        <dependency>
            <groupId>com.gogidix.platform</groupId>
            <artifactId>gogidix-common-messaging</artifactId>
        </dependency>

        <!-- Persistence Library - JPA, Auditing -->
        <dependency>
            <groupId>com.gogidix.platform</groupId>
            <artifactId>gogidix-common-persistence</artifactId>
        </dependency>

        <!-- ===================================================== -->
        <!-- SPRING BOOT STARTERS -->
        <!-- ===================================================== -->

        <!-- Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Data JPA Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Redis Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Kafka Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-kafka</artifactId>
        </dependency>

        <!-- Validation Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Actuator Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- ===================================================== -->
        <!-- DATABASE DRIVERS -->
        <!-- ===================================================== -->

        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- H2 Database (for testing) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- ===================================================== -->
        <!-- ENTERPRISE LIBRARIES -->
        <!-- ===================================================== -->

        <!-- Flyway Migration -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- Apache Commons Lang -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- ===================================================== -->
        <!-- TESTING DEPENDENCIES -->
        <!-- ===================================================== -->

        <!-- Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Testcontainers -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Testcontainers Kafka -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Testcontainers Redis -->
        <dependency>
            <groupId>com.playtika.testcontainers</groupId>
            <artifactId>embedded-redis</artifactId>
            <version>3.1.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- BUILD CONFIGURATION -->
    <build>
        <finalName>${service.name}</finalName>

        <plugins>
            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>${main.class}</mainClass>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                    <image>
                        <name>${docker.image.prefix}/${service.name}:${project.version}</name>
                        <publish>true</publish>
                        <env>
                            <SPRING_PROFILES_ACTIVE>prod</SPRING_PROFILES_ACTIVE>
                            <JAVA_OPTS>-Xms512m -Xmx1g</JAVA_OPTS>
                        </env>
                    </image>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                            <goal>build-image</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Resources Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <configuration>
                    <encoding>UTF-8</encoding>
                    <useDefaultDelimiters>false</useDefaultDelimiters>
                    <delimiters>
                        <delimiter>${resource.delimiter}</delimiter>
                    </delimiters>
                </configuration>
            </plugin>

            <!-- Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- JaCoCo Plugin -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
            </plugin>

            <!-- Surefire Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>

            <!-- Failsafe Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <!-- PROFILES -->
    <profiles>
        <!-- Development Profile -->
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>

        <!-- Test Profile -->
        <profile>
            <id>test</id>
            <properties>
                <spring.profiles.active>test</spring.profiles.active>
            </properties>
            <dependencies>
                <!-- Test-specific dependencies -->
            </dependencies>
        </profile>

        <!-- Staging Profile -->
        <profile>
            <id>staging</id>
            <properties>
                <spring.profiles.active>staging</spring.profiles.active>
            </properties>
        </profile>

        <!-- Production Profile -->
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <configuration>
                            <excludeDevtools>true</excludeDevtools>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>

# =====================================================
# APPLICATION.YML INTEGRATION TEMPLATE
# =====================================================
spring:
  application:
    name: ${SERVICE_NAME:billing-invoicing-service}

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  # Import central configuration
  config:
    import:
      - optional:classpath:platform-application-commons/application.yml
      - optional:classpath:platform-application-commons/security.yml
      - optional:classpath:platform-application-commons/monitoring.yml
      - optional:classpath:platform-application-commons/performance.yml
      - optional:classpath:platform-application-commons/external-services.yml
      - optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}

# =====================================================
# SERVICE-SPECIFIC CONFIGURATION
# =====================================================
gogidix:
  platform:
    service:
      name: ${spring.application.name}
      group: financial-management
      domain: billing

    # Business Configuration
    billing:
      # Invoice Configuration
      invoice:
        numbering:
          prefix: INV-${ENV:DEV}
          sequence-start: 1000
          zero-pad: 6
        due-days: ${BILLING_DUE_DAYS:30}
        late-fee:
          percentage: ${BILLING_LATE_FEE:0.05}
          grace-days: ${BILLING_GRACE_DAYS:5}

      # Payment Configuration
      payment:
        providers:
          stripe:
            enabled: ${STRIPE_ENABLED:true}
          paypal:
            enabled: ${PAYPAL_ENABLED:false}
        retry:
          max-attempts: ${PAYMENT_RETRY_MAX:3}
          backoff-delay: PT5M

      # Notification Configuration
      notification:
        email:
          enabled: ${BILLING_EMAIL_ENABLED:true}
          templates:
            invoice-created: billing-invoice-created
            payment-received: billing-payment-received
            payment-failed: billing-payment-failed
            overdue-notice: billing-overdue-notice

    # Domain Events
    events:
      billing:
        invoice-created: billing.invoice.created
        invoice-updated: billing.invoice.updated
        invoice-cancelled: billing.invoice.cancelled
        payment-received: billing.payment.received
        payment-failed: billing.payment.failed
        payment-refunded: billing.payment.refunded

# =====================================================
# DATABASE CONFIGURATION
# =====================================================
spring:
  datasource:
    url: ${BILLING_DB_URL:jdbc:postgresql://localhost:5432/${db.name}}
    username: ${BILLING_DB_USERNAME:billing_user}
    password: ${BILLING_DB_PASSWORD:}
    hikari:
      maximum-pool-size: ${BILLING_DB_POOL_SIZE:20}
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 600000

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: ${BILLING_JPA_DDL_AUTO:validate}
    show-sql: ${BILLING_SHOW_SQL:false}
    properties:
      hibernate:
        default_schema: ${BILLING_DB_SCHEMA:billing}
        format_sql: true

  flyway:
    enabled: ${BILLING_FLYWAY_ENABLED:true}
    locations: ${flyway.locations}
    schemas: ${BILLING_DB_SCHEMA:billing}
    baseline-on-migrate: true
    validate-on-migrate: true

# =====================================================
# KAFKA CONFIGURATION FOR BILLING EVENTS
# =====================================================
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: ${spring.application.name}-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.gogidix.platform.billing.domain.event

# =====================================================
# REDIS CONFIGURATION FOR BILLING CACHE
# =====================================================
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE_BILLING:2}
      timeout: ${REDIS_TIMEOUT:5000ms}

# =====================================================
# EXTERNAL SERVICE CONFIGURATION
# =====================================================
gogidix:
  platform:
    external-services:
      # Payment Gateway
      stripe:
        enabled: ${STRIPE_ENABLED:false}
        api-key: ${STRIPE_API_KEY:}
        webhook-secret: ${STRIPE_WEBHOOK_SECRET:}

      # Email Service
      sendgrid:
        enabled: ${SENDGRID_ENABLED:false}
        api-key: ${SENDGRID_API_KEY:}
        from-email: ${SENDGRID_BILLING_EMAIL:billing@gogidix.com}

# =====================================================
# MONITORING CONFIGURATION
# =====================================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,billing
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
    billing:
      enabled: true
  metrics:
    tags:
      service: ${spring.application.name}
      domain: billing

# =====================================================
# LOGGING CONFIGURATION
# =====================================================
logging:
  level:
    com.gogidix.platform.billing: ${LOGGING_LEVEL_BILLING:INFO}
    org.springframework.kafka: WARN
    org.hibernate.SQL: ${LOGGING_SQL:WARN}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-},%X{spanId:-}] [%thread] %-5level [%logger{36}] - %msg%n"

# =====================================================
# ENVIRONMENT-SPECIFIC OVERRIDES
# =====================================================
---
spring:
  config:
    activate:
      on-profile: dev

logging:
  level:
    com.gogidix.platform.billing: DEBUG
    org.hibernate.SQL: DEBUG

---
spring:
  config:
    activate:
      on-profile: test

spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
  jpa:
    hibernate:
      ddl-auto: create-drop

---
spring:
  config:
    activate:
      on-profile: prod

logging:
  level:
    com.gogidix.platform.billing: INFO
    root: WARN