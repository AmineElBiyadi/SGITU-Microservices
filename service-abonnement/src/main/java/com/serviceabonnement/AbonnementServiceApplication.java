package com.serviceabonnement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AbonnementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbonnementServiceApplication.class, args);
    }
}
