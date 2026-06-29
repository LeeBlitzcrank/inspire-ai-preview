package com.inspire.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
public class InspireAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspireAuthApplication.class, args);
    }
}
