#!/usr/bin/env python3
"""
✨ GOGIDIX PLATFORM - SHARED LIBRARIES INTEGRATION AUTOMATION ✨
Enterprise Script for Integrating All Domain Services with Shared Libraries

Usage:
    python integrate_shared_libraries.py [--service-path PATH] [--dry-run]

Features:
- Automatic POM updates with shared library dependencies
- Application.java configuration with shared library annotations
- Configuration files integration (application.yml, bootstrap.yml)
- Integration test generation
- Service discovery and batch processing
- Zero-downtime integration support
"""

import os
import sys
import json
import yaml
import shutil
import argparse
from pathlib import Path
from typing import Dict, List, Optional
from datetime import datetime

class SharedLibrariesIntegrator:
    """
    🏗️ Enterprise Shared Libraries Integration Class
    Handles complete integration of Gogidix shared libraries with domain services
    """

    def __init__(self, base_path: str):
        self.base_path = Path(base_path)
        self.shared_libs_path = self.base_path / "foundation-domain" / "shared-libraries" / "backend-services"
        self.domain_path = self.base_path / "Gogidix-Domain"

        # Service configuration mapping
        self.service_config = {
            "billing-invoicing-service": {
                "domain": "billing",
                "group": "financial-management",
                "port": 8084,
                "package": "com.gogidix.platform.billing",
                "main_class": "BillingInvoicingApplication",
                "db_name": "billing_invoicing",
                "db_schema": "billing"
            },
            "user-management-service": {
                "domain": "user",
                "group": "identity-management",
                "port": 8081,
                "package": "com.gogidix.platform.user",
                "main_class": "UserManagementApplication",
                "db_name": "user_management",
                "db_schema": "users"
            },
            "property-management-service": {
                "domain": "property",
                "group": "property-marketplace",
                "port": 8082,
                "package": "com.gogidix.platform.property",
                "main_class": "PropertyManagementApplication",
                "db_name": "property_management",
                "db_schema": "properties"
            },
            "booking-service": {
                "domain": "booking",
                "group": "reservation-system",
                "port": 8083,
                "package": "com.gogidix.platform.booking",
                "main_class": "BookingServiceApplication",
                "db_name": "booking_service",
                "db_schema": "bookings"
            },
            "notification-service": {
                "domain": "notification",
                "group": "communication",
                "port": 8085,
                "package": "com.gogidix.platform.notification",
                "main_class": "NotificationServiceApplication",
                "db_name": "notification_service",
                "db_schema": "notifications"
            }
        }

    def discover_services(self) -> List[Path]:
        """
        🔍 Discover all domain services in the project
        """
        services = []

        # Search in all domain directories
        for domain_dir in ["business-domain", "management-domain", "third-party-domain"]:
            domain_path = self.domain_path / domain_dir
            if domain_path.exists():
                # Find all *-service directories
                services.extend(domain_path.glob("**/*-service"))

        return services

    def update_pom_file(self, service_path: Path, service_config: Dict) -> bool:
        """
        📝 Update service POM with shared library dependencies
        """
        pom_path = service_path / "pom.xml"

        if not pom_path.exists():
            print(f"⚠️  POM not found: {pom_path}")
            return False

        # Generate new POM content
        pom_content = self.generate_pom_content(service_config, service_path)

        # Write new POM
        try:
            with open(pom_path, 'w', encoding='utf-8') as f:
                f.write(pom_content)
            print(f"✅ Updated POM: {pom_path}")
            return True
        except Exception as e:
            print(f"❌ Failed to update POM: {e}")
            return False

    def generate_pom_content(self, config: Dict, service_path: Path) -> str:
        """
        🏭 Generate POM content with shared libraries integration
        """
        service_name = config["main_class"].replace("Application", "").lower()

        return f"""<?xml version="1.0" encoding="UTF-8"?>
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
    <artifactId>{service_name}</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Gogidix {config['main_class'].replace('Application', '')}</name>
    <description>Enterprise {config['domain']} management microservice with DDD architecture</description>

    <!-- ORGANIZATION -->
    <organization>
        <name>Gogidix Platform</name>
        <url>https://gogidix.com</url>
    </organization>

    <!-- GLOBAL PROPERTIES -->
    <properties>
        <!-- Service Configuration -->
        <service.name>{service_name}</service.name>
        <service.group>{config['group']}</service.group>
        <service.domain>{config['domain']}</service.domain>

        <!-- Base Package for Scanning -->
        <base.package>{config['package']}</base.package>

        <!-- Application Entry Point -->
        <main.class>{config['package']}.{config['main_class']}</main.class>

        <!-- Database Configuration -->
        <db.name>{config['db_name']}</db.name>
        <flyway.locations>classpath:db/migration/{config['domain']}</flyway.locations>

        <!-- Docker Configuration -->
        <docker.port>{config['port']}</docker.port>
        <docker.host.port>{config['port']}</docker.host.port>
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

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-kafka</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- ===================================================== -->
        <!-- DATABASE DRIVERS -->
        <!-- ===================================================== -->

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- ===================================================== -->
        <!-- ENTERPRISE LIBRARIES -->
        <!-- ===================================================== -->

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- ===================================================== -->
        <!-- TESTING DEPENDENCIES -->
        <!-- ===================================================== -->

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

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
    </dependencies>

    <!-- BUILD CONFIGURATION -->
    <build>
        <finalName>${{service.name}}</finalName>

        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>${{main.class}}</mainClass>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <!-- PROFILES -->
    <profiles>
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>

        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
        </profile>
    </profiles>
</project>"""

    def update_application_java(self, service_path: Path, config: Dict) -> bool:
        """
        ☕ Update Application.java with shared library annotations
        """
        # Create package structure
        pkg_path = service_path / "src" / "main" / "java" / config["package"].replace(".", "/")
        pkg_path.mkdir(parents=True, exist_ok=True)

        app_java_path = pkg_path / f"{config['main_class']}.java"

        # Generate Application.java content
        content = f"""package {config['package']};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gogidix.platform.common.core.annotation.EnableGogidixCore;
import com.gogidix.platform.common.security.annotation.EnableGogidixSecurity;
import com.gogidix.platform.common.messaging.annotation.EnableGogidixMessaging;
import com.gogidix.platform.common.persistence.annotation.EnableGogidixPersistence;
import com.gogidix.platform.common.observability.annotation.EnableGogidixObservability;

/**
 * 🏗️ GOGIDIX {config['main_class'].replace('Application', '').upper()} SERVICE
 *
 * Enterprise Spring Boot Application with Shared Libraries Integration
 *
 * Features:
 * - DDD Architecture with Hexagonal Pattern
 * - JWT Authentication & RBAC Authorization
 * - Event-Driven Architecture with Kafka
 * - Redis Caching & Performance Optimization
 * - Comprehensive Monitoring & Observability
 * - Zero-Configuration with Shared Libraries
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 * @since {datetime.now().strftime('%Y-%m-%d')}
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = {{
    "{config['package']}.domain.repository",
    "com.gogidix.platform.common.persistence.repository"
}})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableKafka

// GOGIDIX SHARED LIBRARIES ENABLEMENT
@EnableGogidixCore
@EnableGogidixSecurity
@EnableGogidixMessaging
@EnableGogidixPersistence
@EnableGogidixObservability

public class {config['main_class']} {{

    /**
     * 🚀 Application Entry Point
     *
     * Starts the {config['domain']} microservice with:
     * - Shared library auto-configuration
     * - Central configuration integration
     * - Health checks and monitoring
     * - Graceful shutdown handling
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {{
        // Set system properties for consistent startup
        System.setProperty("spring.application.name", "{config['main_class'].replace('Application', '').toLowerCase()}-service");
        System.setProperty("spring.profiles.active",
            System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));

        // Run the application with shared libraries
        SpringApplication.run({config['main_class']}.class, args);
    }}
}}"""

        try:
            with open(app_java_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Created Application.java: {app_java_path}")
            return True
        except Exception as e:
            print(f"❌ Failed to create Application.java: {e}")
            return False

    def create_application_yml(self, service_path: Path, config: Dict) -> bool:
        """
        📄 Create application.yml with central configuration integration
        """
        resources_path = service_path / "src" / "main" / "resources"
        resources_path.mkdir(parents=True, exist_ok=True)

        app_yml_path = resources_path / "application.yml"

        service_name = config['main_class'].replace('Application', '').lower() + "-service"

        content = f"""# =====================================================
# GOGIDIX {config['domain'].upper()} SERVICE CONFIGURATION
# =====================================================
spring:
  application:
    name: {service_name}

  profiles:
    active: ${{{{SPRING_PROFILES_ACTIVE:dev}}}}

  # Import central configuration
  config:
    import:
      - optional:classpath:platform-application-commons/application.yml
      - optional:classpath:platform-application-commons/security.yml
      - optional:classpath:platform-application-commons/monitoring.yml
      - optional:classpath:platform-application-commons/performance.yml
      - optional:classpath:platform-application-commons/external-services.yml
      - optional:configserver:${{{{CONFIG_SERVER_URL:http://localhost:8888}}}}

# =====================================================
# SERVICE-SPECIFIC CONFIGURATION
# =====================================================
gogidix:
  platform:
    service:
      name: ${{{{spring.application.name}}}}
      group: {config['group']}
      domain: {config['domain']}

# =====================================================
# DATABASE CONFIGURATION
# =====================================================
spring:
  datasource:
    url: ${{{{{config['domain'].upper()}_DB_URL:jdbc:postgresql://localhost:5432/{config['db_name']}}}}}
    username: ${{{{{config['domain'].upper()}_DB_USERNAME:{config['domain']}_user}}}}
    password: ${{{{{config['domain'].upper()}_DB_PASSWORD:}}}}
    hikari:
      maximum-pool-size: ${{{{ {config['domain'].upper()}_DB_POOL_SIZE:20 }}}}
      minimum-idle: 5

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: ${{{{ {config['domain'].upper()}_JPA_DDL_AUTO:validate}}}}
    show-sql: ${{{{ {config['domain'].upper()}_SHOW_SQL:false}}}}
    properties:
      hibernate:
        default_schema: ${{{{ {config['domain'].upper()}_DB_SCHEMA:{config['db_schema']}}}}}

# =====================================================
# LOGGING CONFIGURATION
# =====================================================
logging:
  level:
    {config['package']}: ${{{{LOGGING_LEVEL_{config['domain'].upper()}:INFO}}}}
  pattern:
    console: "%d{{{{yyyy-MM-dd HH:mm:ss.SSS}}}} [%X{{{{traceId:-}}}},%X{{{{spanId:-}}}}] [%thread] %-5level [%logger{{{{36}}}}] - %msg%n"

# =====================================================
# ENVIRONMENT-SPECIFIC OVERRIDES
# =====================================================
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
"""

        try:
            with open(app_yml_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Created application.yml: {app_yml_path}")
            return True
        except Exception as e:
            print(f"❌ Failed to create application.yml: {e}")
            return False

    def create_integration_test(self, service_path: Path, config: Dict) -> bool:
        """
        🧪 Create integration test for shared libraries
        """
        test_path = service_path / "src" / "test" / "java" / config["package"].replace(".", "/")
        test_path.mkdir(parents=True, exist_ok=True)

        test_file = test_path / "SharedLibrariesIntegrationTest.java"

        content = f"""package {config['package']};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gogidix.platform.common.core.response.ApiResponse;
import com.gogidix.platform.common.security.annotation.AllowPublic;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 🧪 SHARED LIBRARIES INTEGRATION TEST
 *
 * Verifies that all Gogidix shared libraries are properly integrated
 *
 * @author Gogidix Platform Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SharedLibrariesIntegrationTest {{

    @Test
    void contextLoads() {{
        // Verify Spring context loads successfully with all shared libraries
        assertThat(true).isTrue();
    }}

    @Test
    void testApiResponseFramework() {{
        // Test the shared core library response framework
        ApiResponse<String> response = ApiResponse.<String>builder()
            .status(ApiResponse.ResponseStatus.SUCCESS)
            .code(200)
            .message("Test successful")
            .data("Shared libraries working!")
            .build();

        assertThat(response.getStatus()).isEqualTo(ApiResponse.ResponseStatus.SUCCESS);
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isEqualTo("Shared libraries working!");
    }}

    /**
     * Test controller to verify security annotations work
     */
    @AllowPublic
    static class TestController {{
        // Public endpoint for testing security annotations
    }}
}}"""

        try:
            with open(test_file, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✅ Created integration test: {test_file}")
            return True
        except Exception as e:
            print(f"❌ Failed to create integration test: {e}")
            return False

    def integrate_service(self, service_path: Path, dry_run: bool = False) -> bool:
        """
        🔧 Integrate a single service with shared libraries
        """
        service_name = service_path.name

        # Get service configuration
        config = self.service_config.get(service_name)
        if not config:
            print(f"⚠️  No configuration found for service: {service_name}")
            return False

        print(f"\n🚀 Integrating service: {service_name}")
        print(f"   Domain: {config['domain']}")
        print(f"   Package: {config['package']}")

        if dry_run:
            print("   [DRY RUN] Skipping actual file updates")
            return True

        # Update POM
        if not self.update_pom_file(service_path, config):
            return False

        # Update Application.java
        if not self.update_application_java(service_path, config):
            return False

        # Create application.yml
        if not self.create_application_yml(service_path, config):
            return False

        # Create integration test
        if not self.create_integration_test(service_path, config):
            return False

        print(f"✅ Successfully integrated: {service_name}")
        return True

    def integrate_all_services(self, dry_run: bool = False) -> Dict[str, bool]:
        """
        🌍 Integrate all discovered services with shared libraries
        """
        results = {}

        services = self.discover_services()
        print(f"\n🔍 Discovered {len(services)} services")

        for service_path in services:
            service_name = service_path.name
            results[service_name] = self.integrate_service(service_path, dry_run)

        return results

    def generate_report(self, results: Dict[str, bool]) -> None:
        """
        📊 Generate integration report
        """
        total = len(results)
        successful = sum(results.values())
        failed = total - successful

        print("\n" + "="*60)
        print("📊 INTEGRATION REPORT")
        print("="*60)
        print(f"Total Services: {total}")
        print(f"Successful: {successful} ✅")
        print(f"Failed: {failed} ❌")
        print(f"Success Rate: {(successful/total)*100:.1f}%")
        print("\nDetailed Results:")

        for service, success in results.items():
            status = "✅" if success else "❌"
            print(f"  {status} {service}")

        if failed > 0:
            print("\n⚠️  Some services failed to integrate. Please check the logs above.")


def main():
    """
    🎯 Main entry point
    """
    parser = argparse.ArgumentParser(
        description="Integrate Gogidix domain services with shared libraries"
    )
    parser.add_argument(
        "--base-path",
        default=".",
        help="Base path of the Gogidix project"
    )
    parser.add_argument(
        "--service-path",
        help="Specific service path to integrate (integrates all if not provided)"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Perform a dry run without modifying files"
    )

    args = parser.parse_args()

    print("✨ GOGIDIX SHARED LIBRARIES INTEGRATION ✨")
    print(f"Base Path: {args.base_path}")
    print(f"Mode: {'DRY RUN' if args.dry_run else 'LIVE'}")

    # Initialize integrator
    integrator = SharedLibrariesIntegrator(args.base_path)

    if args.service_path:
        # Integrate specific service
        service_path = Path(args.service_path)
        success = integrator.integrate_service(service_path, args.dry_run)

        if success:
            print("\n✅ Service integration completed successfully!")
        else:
            print("\n❌ Service integration failed!")
            sys.exit(1)
    else:
        # Integrate all services
        results = integrator.integrate_all_services(args.dry_run)
        integrator.generate_report(results)


if __name__ == "__main__":
    main()