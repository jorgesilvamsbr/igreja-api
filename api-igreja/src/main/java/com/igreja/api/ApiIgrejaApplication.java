package com.igreja.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiIgrejaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiIgrejaApplication.class, args);
    }

}
