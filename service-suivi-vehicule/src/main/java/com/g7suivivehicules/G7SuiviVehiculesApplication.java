package com.g7suivivehicules;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
public class G7SuiviVehiculesApplication {

    public static void main(String[] args) {
        SpringApplication.run(G7SuiviVehiculesApplication.class, args);
    }

}
