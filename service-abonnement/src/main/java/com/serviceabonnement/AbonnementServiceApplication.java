package com.serviceabonnement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class AbonnementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbonnementServiceApplication.class, args);
    }
}
