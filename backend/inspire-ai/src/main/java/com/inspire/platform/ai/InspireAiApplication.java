package com.inspire.platform.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
public class InspireAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InspireAiApplication.class, args);
    }
}
