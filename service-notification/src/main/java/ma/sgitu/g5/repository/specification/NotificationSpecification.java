package ma.sgitu.g5.repository.specification;

import jakarta.persistence.criteria.Predicate;
import ma.sgitu.g5.entity.Notification;
import ma.sgitu.g5.entity.NotificationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationSpecification — Filtres JPA Criteria dynamiques.
 *
 * Tous les paramètres sont optionnels : seuls les non-null/non-blank
 * sont ajoutés au prédicat. Permet des combinaisons arbitraires sans
 * écrire une requête dédiée par cas.
 *
 * Filtres disponibles :
 *   - userId         : exact match
 *   - status         : exact match (enum NotificationStatus)
 *   - sourceService  : exact match (insensible à la casse — normalisé en UPPER)
 *   - channel        : exact match (EMAIL, SMS, PUSH, LOG)
 *   - startDate      : createdAt >= startDate
 *   - endDate        : createdAt <= endDate
 */
public final class NotificationSpecification {

    private NotificationSpecification() {}

    /**
     * Construit un {@link Specification} combinant tous les filtres fournis.
     *
     * @param userId        filtre sur l'identifiant utilisateur (optionnel)
     * @param status        filtre sur le statut de la notification (optionnel)
     * @param sourceService filtre sur le service source G1-G10 (optionnel)
     * @param channel       filtre sur le canal d'envoi EMAIL/SMS/PUSH/LOG (optionnel)
     * @param startDate     borne inférieure sur createdAt (optionnel)
     * @param endDate       borne supérieure sur createdAt (optionnel)
     */
    public static Specification<Notification> withFilters(
            String userId,
            NotificationStatus status,
            String sourceService,
            String channel,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null && !userId.isBlank()) {
                predicates.add(cb.equal(root.get("userId"), userId.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sourceService != null && !sourceService.isBlank()) {
                predicates.add(cb.equal(root.get("sourceService"),
                        sourceService.trim().toUpperCase()));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(root.get("channel"),
                        channel.trim().toUpperCase()));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
