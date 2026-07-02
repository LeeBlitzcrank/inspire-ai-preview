package com.inspire.platform.core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
@EnableScheduling
public class InspireCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspireCoreApplication.class, args);
    }
}
