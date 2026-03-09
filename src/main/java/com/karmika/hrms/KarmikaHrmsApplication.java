package com.karmika.hrms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@org.springframework.cache.annotation.EnableCaching
public class KarmikaHrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KarmikaHrmsApplication.class, args);
    }
}
