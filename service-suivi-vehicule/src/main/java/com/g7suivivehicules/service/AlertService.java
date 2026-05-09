package com.g7suivivehicules.service;

import com.g7suivivehicules.entity.Alert;
import com.g7suivivehicules.entity.Alert.StatutAlert;
import com.g7suivivehicules.entity.Alert.TypeAlert;
import com.g7suivivehicules.entity.Alert.Severite;
import com.g7suivivehicules.kafka.KafkaProducerService;
import com.g7suivivehicules.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final KafkaProducerService kafkaProducerService;
    private final G5NotificationService g5NotificationService;

    // ========== CRÉATION / MISE À JOUR ==========

    /**
     * Point d'entrée principal appelé par AnomalyDetectionService après chaque
     * anomalie détectée.
     *
     * Logique de déduplication :
     * - Si une alerte OUVERTE ou EN_COURS existe déjà pour ce vehiculeId +
     * typeAlert → met à jour dureeMinutes
     * - Sinon → crée une nouvelle alerte
     *
     * Note : les appels vers KafkaProducerService et G9IntegrationService seront
     * branchés
     * ici lorsque ces services seront disponibles (severite HAUTE ou CRITIQUE).
     */
    @Transactional
    public Alert creerOuMettreAJour(UUID vehiculeId,
            TypeAlert typeAlert,
            Double latitude,
            Double longitude,
            Double valeur,
            Double seuil,
            Severite severite,
            String message) {

        Optional<Alert> existante = alertRepository.findActiveByVehiculeIdAndTypeAlert(vehiculeId, typeAlert);

        if (existante.isPresent()) {
            // Mise à jour de l'alerte existante
            Alert alerte = existante.get();
            long minutes = ChronoUnit.MINUTES.between(alerte.getTimestampDebut(), LocalDateTime.now());
            alerte.setDureeMinutes((int) minutes);
            alerte.setValeur(valeur);
            Alert sauvegardee = alertRepository.save(alerte);
            log.info("[AlertService] Alerte mise à jour — vehiculeId={} type={} durée={}min",
                    vehiculeId, typeAlert, minutes);
            return sauvegardee;
        }

        // Création d'une nouvelle alerte
        Alert nouvelleAlerte = Alert.builder()
                .vehiculeId(vehiculeId)
                .typeAlert(typeAlert)
                .severite(severite)
                .latitude(latitude)
                .longitude(longitude)
                .valeur(valeur)
                .seuil(seuil)
                .message(message)
                .timestampDebut(LocalDateTime.now())
                .statut(StatutAlert.OUVERTE)
                .dureeMinutes(0)
                .envoyeeKafka(false)
                .envoyeeG9(false)
                .build();

        Alert sauvegardee = alertRepository.save(nouvelleAlerte);
        log.warn("[AlertService] Nouvelle alerte créée — vehiculeId={} type={} severite={}",
                vehiculeId, typeAlert, severite);

        // Publication asynchrone sur Kafka (gère l'envoi G4 et G9 en interne)
        kafkaProducerService.publierAlerte(sauvegardee);

        // Notification PUSH au conducteur via G5
        g5NotificationService.notifierConducteur(sauvegardee);

        return sauvegardee;
    }

    // ========== RÉSOLUTION AUTOMATIQUE ==========

    /**
     * Appelé par AnomalyDetectionService quand aucune anomalie n'est détectée
     * et qu'une alerte était encore ouverte → fermeture automatique.
     */
    @Transactional
    public void resoudreAutomatiquement(UUID vehiculeId, TypeAlert typeAlert) {
        alertRepository.findActiveByVehiculeIdAndTypeAlert(vehiculeId, typeAlert)
                .ifPresent(alerte -> {
                    alerte.setStatut(StatutAlert.RESOLUE);
                    alerte.setTimestampFin(LocalDateTime.now());
                    long minutes = ChronoUnit.MINUTES.between(alerte.getTimestampDebut(), alerte.getTimestampFin());
                    alerte.setDureeMinutes((int) minutes);
                    alertRepository.save(alerte);
                    log.info("[AlertService] Alerte résolue automatiquement — vehiculeId={} type={} durée={}min",
                            vehiculeId, typeAlert, minutes);
                });
    }

    // ========== RÉSOLUTION MANUELLE ==========

    /**
     * Appelé par AlertController sur PUT /api/v1/alerts/{id}/resolve
     */
    @Transactional
    public Alert resoudreManuellement(UUID alertId) {
        Alert alerte = trouverParId(alertId);
        if (alerte.getStatut() == StatutAlert.RESOLUE || alerte.getStatut() == StatutAlert.ANNULEE) {
            throw new IllegalStateException("L'alerte est déjà " + alerte.getStatut());
        }
        alerte.setStatut(StatutAlert.RESOLUE);
        alerte.setTimestampFin(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(alerte.getTimestampDebut(), alerte.getTimestampFin());
        alerte.setDureeMinutes((int) minutes);
        Alert sauvegardee = alertRepository.save(alerte);
        log.info("[AlertService] Alerte résolue manuellement — id={}", alertId);
        return sauvegardee;
    }

    // ========== ANNULATION (FAUSSE ALERTE) ==========

    /**
     * Appelé par AlertController sur PUT /api/v1/alerts/{id}/cancel
     */
    @Transactional
    public Alert annuler(UUID alertId) {
        Alert alerte = trouverParId(alertId);
        if (alerte.getStatut() == StatutAlert.RESOLUE || alerte.getStatut() == StatutAlert.ANNULEE) {
            throw new IllegalStateException("L'alerte est déjà " + alerte.getStatut());
        }
        alerte.setStatut(StatutAlert.ANNULEE);
        alerte.setTimestampFin(LocalDateTime.now());
        Alert sauvegardee = alertRepository.save(alerte);
        log.info("[AlertService] Alerte annulée (fausse alerte) — id={}", alertId);
        return sauvegardee;
    }

    // ========== LECTURE ==========

    @Transactional(readOnly = true)
    public Alert trouverParId(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Alerte introuvable : " + alertId));
    }

    @Transactional(readOnly = true)
    public List<Alert> listerAvecFiltres(UUID vehiculeId, StatutAlert statut, TypeAlert typeAlert) {
        return alertRepository.findWithFilters(vehiculeId, statut, typeAlert);
    }

    @Transactional(readOnly = true)
    public List<Alert> listerActives() {
        return alertRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Alert> listerParVehicule(UUID vehiculeId) {
        return alertRepository.findByVehiculeIdOrderByTimestampDebutDesc(vehiculeId);
    }

    @Transactional(readOnly = true)
    public List<Alert> listerActivesParVehicule(UUID vehiculeId) {
        return alertRepository.findActiveByVehiculeId(vehiculeId);
    }

    // ========== STATISTIQUES POUR G8 ==========

    @Transactional(readOnly = true)
    public List<Object[]> statsParType() {
        return alertRepository.countByTypeAlert();
    }

    @Transactional(readOnly = true)
    public List<Object[]> statsParStatut() {
        return alertRepository.countByStatut();
    }

    @Transactional(readOnly = true)
    public List<Object[]> statsParTypeEtStatut() {
        return alertRepository.countByTypeAlertAndStatut();
    }
}
