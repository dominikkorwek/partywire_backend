package com.kumple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KumpleApplication {

    public static void main(String[] args) {
        SpringApplication.run(KumpleApplication.class, args);
    }
}
