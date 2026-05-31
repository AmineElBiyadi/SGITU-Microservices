package ma.sgitu.g5.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import ma.sgitu.g5.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RetryScheduler — Filet de sécurité pour les notifications non relancées (NIVEAU 2).
 *
 * RÔLE dans l'architecture de retry à 3 niveaux :
 * ─────────────────────────────────────────────────
 * NIVEAU 1 — RetryServiceImpl.scheduleRetry() :
 *   Relance immédiate via TaskScheduler avec backoff exponentiel (30s/60s/120s).
 *   Déclenché à chaque échec d'envoi dans handleFailure().
 *
 * NIVEAU 2 — RetryScheduler (@Scheduled) ← CE COMPOSANT :
 *   Polling toutes les 5 minutes (fixedDelay).
 *   Rattrape les notifications FAILED que le Niveau 1 n'aurait pas relancées
 *   (ex: crash du service entre l'échec et le retry planifié, redémarrage, etc.).
 *   C'est le filet de sécurité garantissant la résilience du service.
 *
 * NIVEAU 3 — Dead Letter Topic Kafka (KafkaConfig + KafkaConsumerController) :
 *   Pour les messages Kafka consommés en erreur (côté broker).
 *   Après 3 tentatives Kafka, le message est redirigé vers {topic}.DLT.
 *
 * Cette architecture 3 niveaux assure qu'aucune notification ne se perd
 * même en cas de panne partielle (résilience complète).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    /** Nombre maximum de tentatives avant abandon définitif. */
    private static final int MAX_RETRY_COUNT = 3;

    private final NotificationRepository   notificationRepository;
    private final NotificationServiceImpl  notificationService;

    /**
     * Tâche planifiée toutes les 5 minutes (300 000 ms).
     *
     * Stratégie : fixedDelay (et non fixedRate) pour éviter les chevauchements
     * si un cycle prend plus de 5 minutes (ex: beaucoup de notifications FAILED).
     * Le délai repart toujours après la FIN du cycle précédent.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, MAX_RETRY_COUNT);

        if (failedNotifications.isEmpty()) {
            log.debug("[RetryScheduler] Aucune notification FAILED éligible.");
            return;
        }

        log.info("[RetryScheduler] {} notification(s) FAILED — relance (filet sécurité Niveau 2)",
                failedNotifications.size());

        for (Notification notification : failedNotifications) {
            try {
                log.info("[RetryScheduler] Relance id={} (retryCount={}/{})",
                        notification.getNotificationId(),
                        notification.getRetryCount(),
                        MAX_RETRY_COUNT);

                notification.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(notification);

                // dispatchAsyncFromEntity() est public dans NotificationServiceImpl
                notificationService.dispatchAsyncFromEntity(notification);

            } catch (Exception ex) {
                log.error("[RetryScheduler] Erreur relance id={} : {}",
                        notification.getNotificationId(), ex.getMessage(), ex);
            }
        }

        log.info("[RetryScheduler] Cycle terminé pour {} notification(s).", failedNotifications.size());
    }
}
