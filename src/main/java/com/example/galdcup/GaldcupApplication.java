package com.example.galdcup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GaldcupApplication {
    public static void main(String[] args) {
        SpringApplication.run(GaldcupApplication.class, args);
    }
}
