package com.example.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // for the periodic cache-stats logger
public class CaffeineDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaffeineDemoApplication.class, args);
    }
}
