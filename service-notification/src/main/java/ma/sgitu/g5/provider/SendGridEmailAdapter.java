package ma.sgitu.g5.provider;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.response.SendResultDTO;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendGridEmailAdapter implements IEmailProvider {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@sgitu.ma}")
    private String fromAddress;

    /**
     * Envoie un e-mail via JavaMailSender (SMTP configuré par Spring).
     * Protégé par un Circuit Breaker "notificationProvider" :
     * si le taux d'échec dépasse 50% sur 10 appels, le circuit s'ouvre 30s
     * et la méthode fallback est appelée immédiatement.
     *
     * @param to      adresse e-mail du destinataire
     * @param subject sujet du message
     * @param body    corps du message
     * @return {@link SendResultDTO} avec le résultat de l'envoi
     */
    @Override
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendFallback")
    public SendResultDTO send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            SendResultDTO result = new SendResultDTO();
            result.setSuccess(false);
            result.setErrorCode("EMAIL_MISSING");
            result.setRetryCount(0);
            return result;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject != null ? subject : "Notification SGITU");
            msg.setText(body != null ? body : "");
            mailSender.send(msg);

            log.info("[G5-EMAIL] Envoye -> {} | sujet: {}", to, subject);

            SendResultDTO result = new SendResultDTO();
            result.setSuccess(true);
            result.setProvider("smtp-" + UUID.randomUUID());
            result.setRetryCount(0);
            return result;
        } catch (Exception e) {
            log.error("[G5-EMAIL] Echec -> {} | {}", to, e.getMessage());
            // Relancer pour que le Circuit Breaker comptabilise l'échec
            throw new RuntimeException("[EMAIL] Échec envoi vers " + to + " : " + e.getMessage(), e);
        }
    }

    /**
     * Fallback déclenché par le Circuit Breaker lorsque le circuit est OPEN
     * ou lors d'une exception non rattrapée dans {@link #send}.
     * Retourne un {@link SendResultDTO} d'échec afin que
     * {@code NotificationServiceImpl.handleFailure()} prenne le relais.
     */
    public SendResultDTO sendFallback(String to, String subject, String body, Throwable ex) {
        log.warn("[G5-EMAIL] Circuit Breaker actif — fallback pour {} | cause: {}", to, ex.getMessage());
        SendResultDTO result = new SendResultDTO();
        result.setSuccess(false);
        result.setErrorCode("CIRCUIT_BREAKER_EMAIL_OPEN");
        result.setRetryCount(0);
        return result;
    }
}
