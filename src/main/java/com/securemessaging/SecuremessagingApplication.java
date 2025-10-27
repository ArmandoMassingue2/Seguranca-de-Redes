package com.securemessaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SecuremessagingApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecuremessagingApplication.class, args);
    }
}