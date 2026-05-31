package ma.sgitu.g5.repository;

import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>,
        JpaSpecificationExecutor<Notification> {

    /**
     * Déduplication composite : (sourceService + notificationId) = clé unique globale.
     * Deux groupes différents peuvent envoyer le même UUID sans collision.
     */
    boolean existsBySourceServiceAndNotificationId(String sourceService, String notificationId);

    Optional<Notification> findBySourceServiceAndNotificationId(String sourceService, String notificationId);

    /**
     * Recherche par notificationId seul — utilisé pour le retry via l'API.
     * Retourne le premier résultat (cas normal : un seul par sourceService).
     */
    Optional<Notification> findFirstByNotificationId(String notificationId);

    /**
     * Recherche les notifications FAILED éligibles au retry automatique.
     * Utilisé par RetryScheduler (toutes les 5 minutes).
     */
    java.util.List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetryCount);

    // Méthodes pour l'administration
    long countByStatus(NotificationStatus status);
    long countByChannel(String channel);
    Page<Notification> findByStatusAndChannel(NotificationStatus status, String channel, Pageable pageable);
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
    Page<Notification> findByChannel(String channel, Pageable pageable);
    Page<Notification> findBySourceService(String sourceService, Pageable pageable);
}
