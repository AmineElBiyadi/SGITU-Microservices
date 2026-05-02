package ma.sgitu.payment.client;

import ma.sgitu.payment.dto.request.NotificationRequest;
//import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//@FeignClient(name = "notification-service", url = "${notification.service.url}")
public interface NotificationClient {
    //@PostMapping("notifications/send")
    //void sendNotification(@RequestBody Object notificationRequest);
}