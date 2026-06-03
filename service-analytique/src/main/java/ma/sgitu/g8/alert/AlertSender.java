package ma.sgitu.g8.alert;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class AlertSender {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${g5.notification.url}")
    private String g5Url;

    @CircuitBreaker(name = "g5AlertCircuitBreaker", fallbackMethod = "sendFallback")
    public void send(Map<String, Object> payload) {
        restTemplate.postForObject(g5Url, payload, Void.class);
        log.info("Alert sent to G5: {}", payload.get("eventType"));
    }

    public void sendFallback(Map<String, Object> payload, Throwable ex) {
        log.warn("G5 circuit breaker OPEN — alert dropped [{}]: {}",
                payload.get("eventType"), ex.getMessage());
    }
}
