package ma.sgitu.g5.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g5.dto.request.MetadataDTO;

@Slf4j
@Service
public class TemplateServiceImpl implements ITemplateService {

    // ============================================================
    // TEMPLATES G1 - BILLETTERIE (Kafka - 11 eventTypes)
    // ============================================================
    private static final String TPL_TICKET_ISSUED = "Votre ticket a été émis avec succès. QR Code : {tokenValue}. Expire le {expiresAt}.";

    private static final String TPL_TICKET_VALIDATED = "Validation réussie. Bon voyage ! Ticket : {ticketId}";

    private static final String TPL_TICKET_FLAGGED = "ALERTE : Votre ticket a été signalé comme suspect ({raisonFlag}). Contactez immédiatement un agent.";

    private static final String TPL_TICKET_FLAG_REVIEWED = "Décision administrative : {decision}. Ticket : {ticketId}";

    private static final String TPL_TICKET_CANCELLED = "Votre ticket a été annulé. Aucun montant n'a été débité.";

    private static final String TPL_TICKET_TRANSFER_INITIATED = "Transfert de ticket initié vers {recipientId}. En attente d'acceptation avant expiration.";

    private static final String TPL_TICKET_TRANSFER_COMPLETED = "Transfert accepté avec succès. Votre nouveau ticket est prêt.";

    private static final String TPL_TICKET_TRANSFER_CANCELLED = "Transfert annulé. Raison : {raison}.";

    private static final String TPL_TICKET_REFUND_REQUESTED = "Demande de remboursement en cours de traitement : {montant} MAD.";

    private static final String TPL_TICKET_REFUNDED = "Remboursement de {montant} MAD effectué avec succès.";

    private static final String TPL_TICKET_EXPIRED = "Votre ticket a expiré le {expiredAt}. Merci de votre confiance.";

    // ============================================================
    // TEMPLATES G2 - ABONNEMENTS (Kafka - 13 eventTypes)
    // ============================================================
    private static final String TPL_CONFIRMATION_SOUSCRIPTION = "Abonnement {planNom} activé ! Période : {dateDebut} au {dateFin}. Montant payé : {montantPaye} MAD.";

    private static final String TPL_ECHEC_SOUSCRIPTION = "Échec de la souscription à {planNom}. Motif : {motif}.";

    private static final String TPL_RAPPEL_EXPIRATION = "RAPPEL : Votre abonnement {planNom} expire dans {joursRestants} jours ({dateFin}). Renouvellement auto : {renouvellementAuto}.";

    private static final String TPL_RENOUVELLEMENT_EFFECTUE = "Renouvellement {typeRenouvellement} effectué avec succès. Nouvelle échéance : {nouvelleDateFin}. Montant : {montantPaye} MAD.";

    private static final String TPL_RENOUVELLEMENT_ECHOUE = "Échec du renouvellement de {planNom}. {motif}. Tentative {nbTentatives}/{maxTentatives}.";

    private static final String TPL_ANNULATION_EFFECTUEE = "Abonnement {planNom} résilié. Remboursement : {montantRembourse} MAD.";

    private static final String TPL_ANNULATION_ECHOUE = "Échec de l'annulation de {planNom}. Motif : {motif}.";

    private static final String TPL_SUSPENSION_EFFECTUEE = "ALERTE : Votre abonnement {planNom} a été suspendu par l'administrateur. Motif : {motif}.";

    private static final String TPL_SUSPENSION_ECHOUE = "Échec de la suspension. Motif : {motif}.";

    private static final String TPL_DESACTIVATION_EFFECTUEE = "Désactivation temporaire acceptée : {dateDebutDesactivation} → {dateFinDesactivation}. ({nbDesactivationsUsees}/{maxDesactivations} désactivations utilisées).";

    private static final String TPL_DESACTIVATION_ECHOUEE = "Désactivation refusée. Motif : {motif}.";

    private static final String TPL_MODIFICATION_EFFECTUEE = "Plan {planNom} modifié avec succès. Changements : {changements}. Prochain renouvellement : {prochainRenouvellement}.";

    private static final String TPL_MODIFICATION_ECHOUE = "Échec de la modification du plan. Motif : {motif}.";

    // ============================================================
    // TEMPLATES G3 - UTILISATEURS (Kafka + REST fallback - 4 eventTypes)
    // ============================================================
    private static final String TPL_WELCOME = "Bienvenue sur SGITU, {username} ! Votre compte est maintenant activé.";

    private static final String TPL_PASSWORD_CHANGED = "Votre mot de passe a été modifié avec succès. Si vous n'êtes pas à l'origine de cette action, contactez immédiatement le support.";

    private static final String TPL_ACCOUNT_DEACTIVATED = "Votre compte SGITU a été désactivé. Pour toute question, contactez le support client.";

    // G3 utilise SECURITY_ALERT sans ipAddress (détecté par G10)
    private static final String TPL_G3_SECURITY_ALERT = "Connexion suspecte détectée sur votre compte. Vérifiez votre sécurité et changez votre mot de passe si nécessaire.";

    // ============================================================
    // TEMPLATES G4 - COORDINATION (REST - 6 eventTypes)
    // ============================================================
    private static final String TPL_MISSION_CREATED = "Nouvelle mission {missionCode} créée. Ligne {lineId}, Véhicule {vehicleId}, Chauffeur {driverId}.";

    private static final String TPL_MISSION_UPDATED = "Mission {missionCode} modifiée. Ligne {lineId}, Véhicule {vehicleId}, Chauffeur {driverId}.";

    private static final String TPL_MISSION_STARTED = "Mission {missionCode} démarrée. Ligne {lineId}, Véhicule {vehicleId} en route.";

    private static final String TPL_MISSION_COMPLETED = "Mission {missionCode} terminée avec succès. Ligne {lineId}, Véhicule {vehicleId}.";

    private static final String TPL_MISSION_CANCELLED = "Mission {missionCode} ANNULÉE. Raison : {reason}. Source : {cancelSource}.";

    private static final String TPL_MISSION_STATUS_OVERRIDDEN = "Statut mission {missionCode} modifié manuellement : {oldStatus} → {newStatus}. Raison : {reason}.";

    // ============================================================
    // TEMPLATES G6 - PAIEMENT (REST - 5 eventTypes)
    // ============================================================
    private static final String TPL_PAYMENT_METHOD_OTP = "Votre code de vérification OTP est : {otpCode}. Ce code expire dans 10 minutes.";

    private static final String TPL_PAYMENT_SUCCESS = "Votre paiement de {amount} MAD a été validé avec succès. Méthode : {paymentMethod}. Facture N° {invoiceNumber}.";

    private static final String TPL_PAYMENT_FAILED = "Votre paiement a échoué. Raison : {failureReason}. Veuillez réessayer ou contacter le support.";

    private static final String TPL_PAYMENT_CANCELLED = "Votre paiement de {amount} MAD a été annulé. Aucun montant n'a été débité.";

    private static final String TPL_INVOICE_GENERATED = "Votre facture N° {invoiceNumber} est disponible. Montant : {amount} MAD.";

    // ============================================================
    // TEMPLATES G8 - ANALYTICS (REST - 7 eventTypes)
    // ============================================================
    private static final String TPL_PUNCTUALITY_ALERT = "[SGITU] Taux de ponctualité critique - Ligne {lineId}. Valeur : {value}% (seuil : {threshold}%). Période : {period}.";

    private static final String TPL_HIGH_INCIDENT_VOLUME = "[SGITU] Nombre d'incidents élevé - {date}. {value} incidents (seuil : {threshold}).";

    private static final String TPL_INCIDENT_ZONE_RISK = "[SGITU] Zone à risque détectée - Zone {zoneId}. {value} incidents répétés.";

    private static final String TPL_HIGH_CHURN_RATE = "[SGITU] Taux d'attrition élevé - {month}. Churn : {value}% (seuil : {threshold}%).";

    private static final String TPL_LOW_DAILY_REVENUE = "[SGITU] Revenu journalier bas - {date}. {value} MAD (moyenne : {threshold} MAD).";

    private static final String TPL_REPORT_GENERATED = "[SGITU] Rapport analytique disponible - {period}. Consultez le tableau de bord G8.";

    private static final String TPL_CHURN_PREDICTION_ALERT = "[SGITU] Prédiction ML : {value} abonnés à risque de résiliation.";

    // ============================================================
    // TEMPLATES G9 - INCIDENTS (REST - 12 eventTypes)
    // ============================================================
    private static final String TPL_INCIDENT_CONFIRMATION = "Votre signalement {reference} a été enregistré. Type : {type}. Statut : {statut}. Suivi : {lienSuivi}";

    private static final String TPL_INCIDENT_AFFECTATION = "Vous êtes affecté à l'incident {reference}. Gravité : {gravite}. Localisation : {localisation}. Délai : {delaiTraitement}.";

    private static final String TPL_INCIDENT_AFFECTATION_TECHNICIEN = "Intervention requise : {typePanne} sur {equipement} à {localisation}. Urgence : {urgence}.";

    private static final String TPL_INCIDENT_CHANGEMENT_STATUT = "Incident {reference} : passage de {ancienStatut} → {nouveauStatut}. {commentaire}";

    private static final String TPL_INCIDENT_ESCALADE = "ESCALADE CRITIQUE - Incident {reference}. Motif : {motif}. Responsable actuel : {responsableActuel}.";

    private static final String TPL_INCIDENT_CLOTURE = "Incident {reference} clôturé. Résolution : {resolution}. Durée : {delaiResolution}. Satisfaction : {lienSatisfaction}";

    private static final String TPL_INCIDENT_MAINTENANCE_REQUISE = "Maintenance requise : {typeIntervention} sur {equipementConcerne} à {localisation}. Urgence : {urgence}.";

    private static final String TPL_INCIDENT_MAINTENANCE_TERMINEE = "Maintenance terminée sur {reference}. Action : {actionRealisee}. Durée : {dureeIntervention}. Technicien : {technicien}.";

    private static final String TPL_INCIDENT_SUIVI = "Suivi incident {reference} : {duree} en cours. Statut : {statut}. {lienSuivi}";

    private static final String TPL_INCIDENT_RAPPEL = "RAPPEL - Incident {reference}. Temps restant : {tempsRestant}. {lienTraitement}";

    private static final String TPL_INCIDENT_IOT_ALERTE = "ALERTE IOT - {type} détecté à {localisation}. Confiance : {niveauConfiance}. Données : {donneesCapteur}.";

    private static final String TPL_INCIDENT_CONFIRMATION_MAINTENANCE = "Technicien {technicien} confirmé. Arrivée estimée : {heureArriveeEstimee}. Incident : {reference}.";

    // ============================================================
    // TEMPLATES G9 - KAFKA (Contrat v5.0 - 6 eventTypes)
    // ============================================================
    private static final String TPL_G9_K_INCIDENT_ALERT = "ALERTE INCIDENT : {type} à {localisation}. Gravité : {gravite}.";

    private static final String TPL_G9_K_RENFORT_ASSIGNED = "Renfort {renfortType} assigné pour l'incident {reference}.";

    private static final String TPL_G9_K_INTERVENTION_ASSIGNED = "Vous avez été assigné à l'intervention {reference}. Localisation : {localisation}.";

    private static final String TPL_G9_K_STATUS_UPDATED = "Statut de l'incident {reference} mis à jour : {nouveauStatut}.";

    private static final String TPL_G9_K_RESOLVED = "L'incident {reference} est maintenant résolu. Résolution : {resolution}.";

    private static final String TPL_G9_K_CRITICAL_ALERT = "ALERTE CRITIQUE : Incident {reference} nécessite une attention immédiate ! Motif : {motif}.";

    // ============================================================
    // TEMPLATES G10 - AUTH (REST - 3 eventTypes)
    // ============================================================
    private static final String TPL_VERIFY_EMAIL = "Bienvenue sur SGITU ! Cliquez sur ce lien pour vérifier votre compte : {verificationLink}";

    private static final String TPL_RESET_PASSWORD = "Réinitialisation de mot de passe demandée. Cliquez sur : {resetLink}. Ce lien expire dans 1 heure.";

    // G10 utilise SECURITY_ALERT avec ipAddress (détecté par G10)
    private static final String TPL_G10_SECURITY_ALERT = "Connexion suspecte détectée sur votre compte depuis l'IP {ipAddress}. Si ce n'est pas vous, changez votre mot de passe immédiatement.";

    // ============================================================
    // TEMPLATES G7 - SUIVI VEHICULES (LOG ALERT ADMIN)
    // ============================================================
    private static final String TPL_LOG_ALERT_ADMIN = "ALERTE LOG {logLevel} - {serviceName} : {message} (source={source}, timestamp={timestamp}).";

    // ============================================================
    // MAP : eventType → template message (61 templates)
    // ============================================================
    private static final Map<String, String> MESSAGE_TEMPLATES = Map.ofEntries(
            // G1 - Billetterie (11)
            Map.entry("TICKET_ISSUED", TPL_TICKET_ISSUED),
            Map.entry("TICKET_VALIDATED", TPL_TICKET_VALIDATED),
            Map.entry("TICKET_FLAGGED", TPL_TICKET_FLAGGED),
            Map.entry("TICKET_FLAG_REVIEWED", TPL_TICKET_FLAG_REVIEWED),
            Map.entry("TICKET_CANCELLED", TPL_TICKET_CANCELLED),
            Map.entry("TICKET_TRANSFER_INITIATED", TPL_TICKET_TRANSFER_INITIATED),
            Map.entry("TICKET_TRANSFER_COMPLETED", TPL_TICKET_TRANSFER_COMPLETED),
            Map.entry("TICKET_TRANSFER_CANCELLED", TPL_TICKET_TRANSFER_CANCELLED),
            Map.entry("TICKET_REFUND_REQUESTED", TPL_TICKET_REFUND_REQUESTED),
            Map.entry("TICKET_REFUNDED", TPL_TICKET_REFUNDED),
            Map.entry("TICKET_EXPIRED", TPL_TICKET_EXPIRED),

            // G2 - Abonnements (13)
            Map.entry("CONFIRMATION_SOUSCRIPTION", TPL_CONFIRMATION_SOUSCRIPTION),
            Map.entry("ECHEC_SOUSCRIPTION", TPL_ECHEC_SOUSCRIPTION),
            Map.entry("RAPPEL_EXPIRATION", TPL_RAPPEL_EXPIRATION),
            Map.entry("RENOUVELLEMENT_EFFECTUE", TPL_RENOUVELLEMENT_EFFECTUE),
            Map.entry("RENOUVELLEMENT_ECHOUE", TPL_RENOUVELLEMENT_ECHOUE),
            Map.entry("ANNULATION_EFFECTUEE", TPL_ANNULATION_EFFECTUEE),
            Map.entry("ANNULATION_ECHOUE", TPL_ANNULATION_ECHOUE),
            Map.entry("SUSPENSION_EFFECTUEE", TPL_SUSPENSION_EFFECTUEE),
            Map.entry("SUSPENSION_ECHOUE", TPL_SUSPENSION_ECHOUE),
            Map.entry("DESACTIVATION_EFFECTUEE", TPL_DESACTIVATION_EFFECTUEE),
            Map.entry("DESACTIVATION_ECHOUEE", TPL_DESACTIVATION_ECHOUEE),
            Map.entry("MODIFICATION_EFFECTUEE", TPL_MODIFICATION_EFFECTUEE),
            Map.entry("MODIFICATION_ECHOUE", TPL_MODIFICATION_ECHOUE),

            // G3 - Utilisateurs (4) - SECURITY_ALERT sans ipAddress
            Map.entry("WELCOME", TPL_WELCOME),
            Map.entry("PASSWORD_CHANGED", TPL_PASSWORD_CHANGED),
            Map.entry("ACCOUNT_DEACTIVATED", TPL_ACCOUNT_DEACTIVATED),
            Map.entry("SECURITY_ALERT", TPL_G3_SECURITY_ALERT),

            // G4 - Coordination (6)
            Map.entry("MISSION_CREATED", TPL_MISSION_CREATED),
            Map.entry("MISSION_UPDATED", TPL_MISSION_UPDATED),
            Map.entry("MISSION_STARTED", TPL_MISSION_STARTED),
            Map.entry("MISSION_COMPLETED", TPL_MISSION_COMPLETED),
            Map.entry("MISSION_CANCELLED", TPL_MISSION_CANCELLED),
            Map.entry("MISSION_STATUS_OVERRIDDEN", TPL_MISSION_STATUS_OVERRIDDEN),

            // G6 - Paiement (5)
            Map.entry("PAYMENT_METHOD_OTP", TPL_PAYMENT_METHOD_OTP),
            Map.entry("PAYMENT_SUCCESS", TPL_PAYMENT_SUCCESS),
            Map.entry("PAYMENT_FAILED", TPL_PAYMENT_FAILED),
            Map.entry("PAYMENT_CANCELLED", TPL_PAYMENT_CANCELLED),
            Map.entry("INVOICE_GENERATED", TPL_INVOICE_GENERATED),

            // G8 - Analytics (7)
            Map.entry("PUNCTUALITY_ALERT", TPL_PUNCTUALITY_ALERT),
            Map.entry("HIGH_INCIDENT_VOLUME", TPL_HIGH_INCIDENT_VOLUME),
            Map.entry("INCIDENT_ZONE_RISK", TPL_INCIDENT_ZONE_RISK),
            Map.entry("HIGH_CHURN_RATE", TPL_HIGH_CHURN_RATE),
            Map.entry("LOW_DAILY_REVENUE", TPL_LOW_DAILY_REVENUE),
            Map.entry("REPORT_GENERATED", TPL_REPORT_GENERATED),
            Map.entry("CHURN_PREDICTION_ALERT", TPL_CHURN_PREDICTION_ALERT),

            // G9 - Incidents (12)
            Map.entry("INCIDENT_CONFIRMATION", TPL_INCIDENT_CONFIRMATION),
            Map.entry("INCIDENT_AFFECTATION", TPL_INCIDENT_AFFECTATION),
            Map.entry("INCIDENT_AFFECTATION_TECHNICIEN", TPL_INCIDENT_AFFECTATION_TECHNICIEN),
            Map.entry("INCIDENT_CHANGEMENT_STATUT", TPL_INCIDENT_CHANGEMENT_STATUT),
            Map.entry("INCIDENT_ESCALADE", TPL_INCIDENT_ESCALADE),
            Map.entry("INCIDENT_CLOTURE", TPL_INCIDENT_CLOTURE),
            Map.entry("INCIDENT_MAINTENANCE_REQUISE", TPL_INCIDENT_MAINTENANCE_REQUISE),
            Map.entry("INCIDENT_MAINTENANCE_TERMINEE", TPL_INCIDENT_MAINTENANCE_TERMINEE),
            Map.entry("INCIDENT_SUIVI", TPL_INCIDENT_SUIVI),
            Map.entry("INCIDENT_RAPPEL", TPL_INCIDENT_RAPPEL),
            Map.entry("INCIDENT_IOT_ALERTE", TPL_INCIDENT_IOT_ALERTE),
            Map.entry("INCIDENT_CONFIRMATION_MAINTENANCE", TPL_INCIDENT_CONFIRMATION_MAINTENANCE),

            // G9 - Kafka (6)
            Map.entry("INCIDENT_ALERT", TPL_G9_K_INCIDENT_ALERT),
            Map.entry("RENFORT_ASSIGNED", TPL_G9_K_RENFORT_ASSIGNED),
            Map.entry("INTERVENTION_ASSIGNED", TPL_G9_K_INTERVENTION_ASSIGNED),
            Map.entry("INCIDENT_STATUS_UPDATED", TPL_G9_K_STATUS_UPDATED),
            Map.entry("INCIDENT_RESOLVED", TPL_G9_K_RESOLVED),
            Map.entry("CRITICAL_ALERT", TPL_G9_K_CRITICAL_ALERT),

            // G10 - Auth (3) - pas de SECURITY_ALERT ici car déjà mappé dans G3
            Map.entry("VERIFY_EMAIL", TPL_VERIFY_EMAIL),
            Map.entry("RESET_PASSWORD", TPL_RESET_PASSWORD),

            // G7 - Logs admin
            Map.entry("LOG_ALERT_ADMIN", TPL_LOG_ALERT_ADMIN));

    // ============================================================
    // MAP : eventType → template subject (pour emails)
    // ============================================================
    private static final Map<String, String> SUBJECT_TEMPLATES = Map.ofEntries(
            // G1
            Map.entry("TICKET_ISSUED", "SGITU - Ticket émis"),
            Map.entry("TICKET_FLAGGED", "SGITU - ALERTE Ticket"),
            Map.entry("TICKET_REFUNDED", "SGITU - Remboursement effectué"),

            // G2
            Map.entry("CONFIRMATION_SOUSCRIPTION", "SGITU - Abonnement activé"),
            Map.entry("RAPPEL_EXPIRATION", "SGITU - Rappel expiration abonnement"),
            Map.entry("SUSPENSION_EFFECTUEE", "SGITU - ALERTE Suspension abonnement"),

            // G3
            Map.entry("WELCOME", "Bienvenue sur SGITU"),
            Map.entry("SECURITY_ALERT", "SGITU - Alerte de sécurité"),

            // G7
            Map.entry("LOG_ALERT_ADMIN", "SGITU - Alerte logs admin"),

            // G4
            Map.entry("MISSION_CANCELLED", "SGITU - Mission annulée"),
            Map.entry("MISSION_STATUS_OVERRIDDEN", "SGITU - Statut mission modifié"),

            // G6
            Map.entry("PAYMENT_METHOD_OTP", "SGITU - Code de vérification"),
            Map.entry("PAYMENT_SUCCESS", "SGITU - Paiement validé"),
            Map.entry("PAYMENT_FAILED", "SGITU - Paiement échoué"),
            Map.entry("INVOICE_GENERATED", "SGITU - Nouvelle facture"),

            // G8
            Map.entry("PUNCTUALITY_ALERT", "SGITU - Alerte ponctualité"),
            Map.entry("HIGH_INCIDENT_VOLUME", "SGITU - Alerte volume incidents"),
            Map.entry("INCIDENT_ZONE_RISK", "SGITU - Alerte zone à risque"),

            // G9
            Map.entry("INCIDENT_CONFIRMATION", "SGITU - Confirmation de signalement"),
            Map.entry("INCIDENT_AFFECTATION", "SGITU - Affectation incident"),
            Map.entry("INCIDENT_ESCALADE", "SGITU - ALERTE ESCALADE"),
            Map.entry("INCIDENT_CLOTURE", "SGITU - Incident clôturé"),
            Map.entry("INCIDENT_IOT_ALERTE", "SGITU - ALERTE IOT"),

            // G9 - Kafka
            Map.entry("INCIDENT_ALERT", "SGITU - Signalement incident"),
            Map.entry("RENFORT_ASSIGNED", "SGITU - Renfort assigné"),
            Map.entry("INTERVENTION_ASSIGNED", "SGITU - Affectation d'intervention"),
            Map.entry("INCIDENT_STATUS_UPDATED", "SGITU - Statut incident mis à jour"),
            Map.entry("INCIDENT_RESOLVED", "SGITU - Incident résolu"),
            Map.entry("CRITICAL_ALERT", "SGITU - ALERTE CRITIQUE G9"),

            // G10
            Map.entry("VERIFY_EMAIL", "SGITU - Vérification de compte"),
            Map.entry("RESET_PASSWORD", "SGITU - Réinitialisation mot de passe"));

    @Override
    public String hydrateMessage(String eventType, MetadataDTO metadata) {
        String template = MESSAGE_TEMPLATES.get(eventType);
        if (template == null) {
            log.warn("Template non trouvé pour eventType: {}", eventType);
            return "Notification SGITU - Événement : " + eventType;
        }
        return replaceVariables(template, metadata != null ? metadata.getData() : null);
    }

    @Override
    public String hydrateSubject(String eventType, MetadataDTO metadata) {
        String template = SUBJECT_TEMPLATES.get(eventType);
        if (template == null) {
            return "Notification SGITU";
        }
        return replaceVariables(template, metadata != null ? metadata.getData() : null);
    }

    /**
     * Remplace {variable} par la valeur correspondante dans metadata
     */
    private String replaceVariables(String template, Map<String, Object> data) {
        if (data == null || template == null) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        // Supprime les placeholders non remplacés
        result = result.replaceAll("\\{[^}]+\\}", "");

        return result;
    }
}