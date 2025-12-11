package com.gogidix.infrastructure.eventsourcing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for Event Sourcing Service.
 *
 * This service provides event sourcing capabilities for maintaining
 * a complete audit trail of state changes, enabling event-driven
 * architecture and temporal data queries.
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