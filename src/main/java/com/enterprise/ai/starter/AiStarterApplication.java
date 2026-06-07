package com.enterprise.ai.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AiStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStarterApplication.class, args);
    }
}
