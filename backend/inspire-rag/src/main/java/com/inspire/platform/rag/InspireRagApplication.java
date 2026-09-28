package com.inspire.platform.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.inspire.platform")
@ConfigurationPropertiesScan
@EnableScheduling
public class InspireRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspireRagApplication.class, args);
    }
}
