package ma.sgitu.g5.provider;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.response.SendResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LogFileAdapter — Provider de notification de type LOG.
 *
 * Implémente {@link ILogProvider} pour écrire des notifications admin
 * sous forme d'entrées structurées dans un fichier log dédié.
 *
 * POURQUOI UN ADAPTER LOG DISTINCT ?
 * ────────────────────────────────────
 * Le professeur précise que certaines notifications ne sont pas des messages
 * (EMAIL/SMS/PUSH) mais des FICHIERS LOGS au niveau admin. Ce provider
 * représente ce canal en routant vers un logger dédié (admin-notifications)
 * configuré dans logback-admin.xml avec rotation quotidienne.
 *
 * FORMAT D'ENTRÉE LOG :
 * [NOTIF-LOG] 2026-05-31T15:00:00 | SOURCE=G7_SUIVI_VEHICULES | USER=admin | SUBJECT=ALERTE_CRITIQUE | MESSAGE=...
 *
 * FICHIER CIBLE : logs/admin-notifications.log (rotation quotidienne, 30 jours)
 */
@Slf4j
@Component
public class LogFileAdapter implements ILogProvider {

    /**
     * Logger dédié aux notifications de type LOG.
     * Mappé dans logback-admin.xml vers logs/admin-notifications.log
     */
    private static final Logger NOTIF_LOG =
            LoggerFactory.getLogger("ma.sgitu.g5.notifications.log");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public SendResultDTO log(String subject, String message, String sourceService, String userId) {
        try {
            String timestamp = LocalDateTime.now().format(FORMATTER);

            // Écriture dans le fichier log dédié (admin-notifications.log)
            NOTIF_LOG.info("[NOTIF-LOG] {} | SOURCE={} | USER={} | SUBJECT={} | MESSAGE={}",
                    timestamp,
                    sourceService != null ? sourceService : "UNKNOWN",
                    userId        != null ? userId        : "system",
                    subject       != null ? subject       : "N/A",
                    message       != null ? message       : "");

            // Log applicatif standard pour suivi
            log.info("[G5-LOG-PROVIDER] Notification LOG écrite | source={} | user={} | subject={}",
                    sourceService, userId, subject);

            SendResultDTO ok = new SendResultDTO();
            ok.setSuccess(true);
            ok.setProvider("LOG_FILE");
            return ok;

        } catch (Exception ex) {
            log.error("[G5-LOG-PROVIDER] Erreur écriture notification LOG : {}", ex.getMessage(), ex);
            SendResultDTO err = new SendResultDTO();
            err.setSuccess(false);
            err.setErrorCode("LOG_WRITE_ERROR: " + ex.getMessage());
            err.setProvider("LOG_FILE");
            return err;
        }
    }
}
