package ma.sgitu.g5.provider;

import ma.sgitu.g5.dto.response.SendResultDTO;

/**
 * ILogProvider — Interface du provider de notification de type LOG.
 *
 * Utilisé pour les notifications admin qui doivent être tracées sous forme
 * de fichiers logs structurés (et non envoyées par EMAIL/SMS/PUSH).
 *
 * Cas d'usage :
 *  - Alertes sécurité critiques
 *  - Événements d'audit inter-groupes
 *  - Incidents système à tracer côté admin
 */
public interface ILogProvider {

    /**
     * Écrit une entrée de log structurée dans logs/admin-notifications.log.
     *
     * @param subject   titre / catégorie de l'événement
     * @param message   corps de la notification log
     * @param sourceService service G1-G10 qui a émis la notification
     * @param userId    identifiant de l'utilisateur concerné (ou "system")
     * @return résultat de l'écriture log
     */
    SendResultDTO log(String subject, String message, String sourceService, String userId);
}
