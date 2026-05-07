package ma.sgitu.g8;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class G8AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(G8AnalyticsApplication.class, args);
    }
}
