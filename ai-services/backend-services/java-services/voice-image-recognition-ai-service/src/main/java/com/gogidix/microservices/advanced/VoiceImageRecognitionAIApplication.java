package com.gogidix.microservices.advanced;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Application for Voice and Image Recognition AI Service
 * Part of the AI Services ecosystem for advanced AI features
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableJpaRepositories
public class VoiceImageRecognitionAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceImageRecognitionAIApplication.class, args);
    }
}