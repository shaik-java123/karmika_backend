package com.karmika.hrms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KarmikaHrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KarmikaHrmsApplication.class, args);
        System.out.println("🚀 Karmika HRMS is running on http://localhost:8080");
    }
}
