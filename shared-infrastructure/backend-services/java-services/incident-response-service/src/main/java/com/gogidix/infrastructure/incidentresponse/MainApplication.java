package com.gogidix.infrastructure.incidentresponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Incident Response Service.
 *
 * This service provides automated incident detection, response coordination,
* and remediation workflows for handling system outages, security breaches,
 * and operational issues across the platform.
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