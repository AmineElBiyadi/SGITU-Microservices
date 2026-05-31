package ma.sgitu.g5.provider;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.response.SendResultDTO;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class FCMPushAdapter implements IPushProvider {

    /**
     * Envoie une notification Push via FCM.
     * Protégé par un Circuit Breaker "notificationProvider" :
     * si le taux d'échec dépasse 50% sur 10 appels, le circuit s'ouvre 30s
     * et la méthode fallback est appelée immédiatement.
     *
     * @param deviceToken token FCM du device cible
     * @param message     contenu de la notification push
     * @return {@link SendResultDTO} avec le résultat de l'envoi
     */
    @Override
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendFallback")
    public SendResultDTO send(String deviceToken, String message) {
        if (deviceToken == null || deviceToken.isBlank()) {
            SendResultDTO result = new SendResultDTO();
            result.setSuccess(false);
            result.setErrorCode("TOKEN_MISSING");
            result.setRetryCount(0);
            return result;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            SendResultDTO result = new SendResultDTO();
            result.setSuccess(false);
            result.setErrorCode("FCM_NOT_INITIALIZED");
            result.setRetryCount(0);
            return result;
        }

        try {
            Message msg = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                    .setTitle("SGITU Notification")
                    .setBody(message != null ? message : "")
                    .build())
                .build();

            String response = FirebaseMessaging.getInstance().send(msg);

            SendResultDTO result = new SendResultDTO();
            result.setSuccess(true);
            result.setProvider("fcm-" + response);
            result.setRetryCount(0);
            return result;
        } catch (Exception e) {
            log.error("[G5-PUSH] Echec -> token={} | {}", deviceToken, e.getMessage());
            throw new RuntimeException("[PUSH] Echec envoi FCM : " + e.getMessage(), e);
        }
    }

    /**
     * Fallback déclenché par le Circuit Breaker lorsque le circuit est OPEN
     * ou lors d'une exception non rattrapée dans {@link #send}.
     * Retourne un {@link SendResultDTO} d'échec afin que
     * {@code NotificationServiceImpl.handleFailure()} prenne le relais.
     */
    public SendResultDTO sendFallback(String deviceToken, String message, Throwable ex) {
        log.warn("[G5-PUSH] Circuit Breaker actif — fallback pour token={} | cause: {}", deviceToken, ex.getMessage());
        SendResultDTO result = new SendResultDTO();
        result.setSuccess(false);
        result.setErrorCode("CIRCUIT_BREAKER_PUSH_OPEN");
        result.setRetryCount(0);
        return result;
    }
}
