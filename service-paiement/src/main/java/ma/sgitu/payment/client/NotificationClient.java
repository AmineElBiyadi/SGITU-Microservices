package ma.sgitu.payment.client;

import ma.sgitu.payment.dto.request.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        url = "${notification.service.url:http://notification-service:8085}"
)
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest request);
}