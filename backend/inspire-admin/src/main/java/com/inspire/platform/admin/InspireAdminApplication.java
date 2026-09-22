package com.inspire.platform.admin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@SpringBootApplication(scanBasePackages = "com.inspire.platform")
@EnableCaching
public class InspireAdminApplication {
    public static void main(String[] args) { SpringApplication.run(InspireAdminApplication.class, args); }
}
