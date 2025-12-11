package com.gogidix.foundation.dynamic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.gogidix.foundation.dynamic"})
@EnableCaching
@EnableAsync
@EnableScheduling
public class DynamicconfigserviceApplication {

    public static void main(String[] args) {
        System.setProperty("server.port", "8889");
        SpringApplication.run(DynamicconfigserviceApplication.class, args);
    }
}