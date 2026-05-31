package ma.sgitu.g5.provider;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.response.SendResultDTO;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Service
@Slf4j
public class TwilioSMSAdapter implements ISMSProvider {

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-phone:}")
    private String fromPhone;

    /**
     * Envoie un SMS via Twilio.
     * Protégé par un Circuit Breaker "notificationProvider" :
     * si le taux d'échec dépasse 50% sur 10 appels, le circuit s'ouvre 30s
     * et la méthode fallback est appelée immédiatement.
     *
     * @param phone   numéro de téléphone du destinataire
     * @param message contenu du SMS
     * @return {@link SendResultDTO} avec le résultat de l'envoi
     */
    @Override
    @CircuitBreaker(name = "notificationProvider", fallbackMethod = "sendFallback")
    public SendResultDTO send(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            SendResultDTO result = new SendResultDTO();
            result.setSuccess(false);
            result.setErrorCode("PHONE_MISSING");
            result.setRetryCount(0);
            return result;
        }

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()
                || fromPhone == null || fromPhone.isBlank()) {
            SendResultDTO result = new SendResultDTO();
            result.setSuccess(false);
            result.setErrorCode("TWILIO_NOT_CONFIGURED");
            result.setRetryCount(0);
            return result;
        }

        try {
            Twilio.init(accountSid, authToken);
            Message msg = Message.creator(
                    new PhoneNumber(phone),
                    new PhoneNumber(fromPhone),
                    message != null ? message : "")
                .create();

            log.info("[G5-SMS] Envoye -> {} | sid={}", phone, msg.getSid());

            SendResultDTO result = new SendResultDTO();
            result.setSuccess(true);
            result.setProvider("twilio-" + msg.getSid());
            result.setRetryCount(0);
            return result;
        } catch (Exception e) {
            log.error("[G5-SMS] Echec -> {} | {}", phone, e.getMessage());
            throw new RuntimeException("[SMS] Echec envoi Twilio : " + e.getMessage(), e);
        }
    }

    /**
     * Fallback déclenché par le Circuit Breaker lorsque le circuit est OPEN
     * ou lors d'une exception non rattrapée dans {@link #send}.
     * Retourne un {@link SendResultDTO} d'échec afin que
     * {@code NotificationServiceImpl.handleFailure()} prenne le relais.
     */
    public SendResultDTO sendFallback(String phone, String message, Throwable ex) {
        log.warn("[G5-SMS] Circuit Breaker actif — fallback pour {} | cause: {}", phone, ex.getMessage());
        SendResultDTO result = new SendResultDTO();
        result.setSuccess(false);
        result.setErrorCode("CIRCUIT_BREAKER_SMS_OPEN");
        result.setRetryCount(0);
        return result;
    }
}
