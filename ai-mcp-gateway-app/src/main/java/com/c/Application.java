package com.c;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    @Bean
    public CommandLineRunner mappingCheck(RequestMappingHandlerMapping mapping) {
        return args -> {
            mapping.getHandlerMethods().forEach((k, v) -> {
                System.out.println(k);
            });
        };
    }
}