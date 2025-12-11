package com.gogidix.infrastructure.resourcemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Resource Management Service.
 *
 * This service provides intelligent resource allocation, monitoring,
 * and optimization capabilities for compute, storage, network, and
 * other infrastructure resources across the platform.
 *
 * @author Gogidix Infrastructure Team
 * @version 1.0.0
 */
@SpringBootApplication
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}