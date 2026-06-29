package com.inspire.platform.core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
public class InspireCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspireCoreApplication.class, args);
    }
}
