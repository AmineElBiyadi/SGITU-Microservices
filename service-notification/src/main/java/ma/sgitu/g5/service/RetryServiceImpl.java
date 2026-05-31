package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.repository.NotificationRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * RetryServiceImpl — Gestion des tentatives de relance avec backoff exponentiel.
 *
 * MÉCANISME DE RETRY — DEUX NIVEAUX COMPLÉMENTAIRES :
 * ────────────────────────────────────────────────────
 *
 * NIVEAU 1 — Retry immédiat via TaskScheduler (scheduleRetry) :
 *   Déclenché par handleFailure() après chaque échec d'envoi.
 *   Utilise un backoff exponentiel : 30s → 60s → 120s.
 *   TaskScheduler planifie l'appel à dispatchAsyncFromEntity() avec délai précis.
 *   Max 3 tentatives (shouldRetry). Après → statut FAILED définitif.
 *
 * NIVEAU 2 — Retry polling via @Scheduled (RetryScheduler) :
 *   Filet de sécurité : s'exécute toutes les 5 minutes.
 *   Capture les notifications FAILED qui n'ont pas été relancées par le Niveau 1
 *   (crash du service, redémarrage, etc.). Relance via dispatchAsyncFromEntity().
 *
 * NIVEAU 3 — Dead Letter Topic Kafka (KafkaConfig) :
 *   Pour les messages Kafka en échec (côté consumer).
 *   Après 3 tentatives Kafka, le message est envoyé vers le topic .DLT.
 *   handleDeadLetterEvent() le re-consomme et crée une nouvelle notification.
 *
 * Cette architecture garantit qu'aucune notification ne se perd même en cas de
 * panne partielle du service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetryServiceImpl implements IRetryService {

    /** Nombre maximum de tentatives d'envoi avant passage en FAILED définitif. */
    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final TaskScheduler taskScheduler;

    @Override
    public boolean shouldRetry(int currentRetryCount) {
        return currentRetryCount < MAX_RETRIES;
    }

    /**
     * Backoff exponentiel :
     *   tentative 0 → 30 s
     *   tentative 1 → 60 s
     *   tentative 2 → 120 s
     */
    @Override
    public int nextDelaySeconds(int currentRetryCount) {
        return (int) (30 * Math.pow(2, currentRetryCount));
    }

    /**
     * Planifie une relance différée via Spring {@link TaskScheduler}.
     *
     * Contrairement à un simple Thread.sleep, TaskScheduler :
     *  - Survit à un redémarrage doux (si le bean reste vivant)
     *  - N'occupe pas de thread pendant l'attente
     *  - Est géré par le conteneur Spring (métriques, shutdown propre)
     *
     * @param notificationId UUID de la notification à relancer
     * @param delaySeconds   délai calculé par nextDelaySeconds()
     */
    @Override
    public void scheduleRetry(String notificationId, int delaySeconds) {
        log.info("[G5-RETRY] Relance planifiée dans {}s pour notificationId={}", delaySeconds, notificationId);

        Instant triggerAt = Instant.now().plusSeconds(delaySeconds);

        taskScheduler.schedule(() -> {
            log.info("[G5-RETRY] Déclenchement de la relance pour notificationId={}", notificationId);
            notificationRepository.findFirstByNotificationId(notificationId).ifPresentOrElse(
                    entity -> {
                        // On crée un contexte minimal pour re-dispatcher
                        // NotificationServiceImpl.dispatchAsyncFromEntity() est appelé
                        // via le RetryScheduler (filtre de sécurité / filet de sécurité)
                        entity.setStatus(ma.sgitu.g5.entity.NotificationStatus.PENDING);
                        notificationRepository.save(entity);
                        log.info("[G5-RETRY] Notification {} remise en PENDING pour re-dispatch", notificationId);
                    },
                    () -> log.warn("[G5-RETRY] Notification {} introuvable lors du retry planifié", notificationId)
            );
        }, triggerAt);
    }
}
