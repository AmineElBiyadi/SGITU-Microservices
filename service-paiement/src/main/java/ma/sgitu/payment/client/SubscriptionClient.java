package ma.sgitu.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "subscription-service",
        url = "${subscription.service.url:http://subscription-service:8082}"
)
public interface SubscriptionClient {

    @PostMapping("/abonnements/paiement/confirmation")
    ResponseEntity<Void> confirmPayment(@RequestBody Map<String, Object> confirmationData);

    @PostMapping("/abonnements/remboursement/confirmation")
    ResponseEntity<Void> confirmRefund(@RequestBody Map<String, Object> confirmationData);

    @GetMapping("/abonnements/users/{userId}/actif")
    ResponseEntity<Map<String, Object>> checkActiveSubscription(@PathVariable Long userId);
}