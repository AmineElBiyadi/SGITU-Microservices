// package com.serviceabonnement.client;

// import com.serviceabonnement.enums.TypeNotification;
// import org.springframework.cloud.openfeign.FeignClient;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestParam;

// @FeignClient(name = "service-notification", url = "${application.config.notification-service-url}")
// public interface NotificationClient {

//     @PostMapping("/api/v1/notifications/send")
//     void sendNotification(@RequestParam("utilisateurId") Long utilisateurId,
//                           @RequestParam("message") String message,
//                           @RequestParam("type") TypeNotification type);
// }
