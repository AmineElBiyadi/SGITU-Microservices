package com.serviceabonnement.scheduler;

import com.serviceabonnement.client.PaiementClient;
import com.serviceabonnement.client.UtilisateurServiceClient;
import com.serviceabonnement.service.AbonnementService;
import com.serviceabonnement.dto.external.PaymentRequestDTO;
import com.serviceabonnement.dto.external.PaymentResponseDTO;
import com.serviceabonnement.dto.external.RefundRequestDTO;
import com.serviceabonnement.dto.external.UserDTO;
import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.enums.StatutAbonnement;
import com.serviceabonnement.producer.SubscriptionEventPublisher;
import com.serviceabonnement.repository.AbonnementRepository;
import com.serviceabonnement.repository.RenouvellementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbonnementScheduler {

    private final AbonnementRepository abonnementRepository;
    private final RenouvellementRepository renouvellementRepository;
    private final AbonnementService abonnementService;
    private final PaiementClient paiementClient;
    private final UtilisateurServiceClient userClient;
    private final SubscriptionEventPublisher eventPublisher;

    private static final int MAX_ATTEMPTS = 5;

    /**
     * Tente de traiter les paiements en attente toutes les 5 minutes.
     */
    @Scheduled(fixedRate = 120000) // 2 minutes (optimized for resilience verification)
    public void checkAndRetryPendingPayments() {
        log.info("Lancement du scheduler de vérification/retry des paiements en attente...");
        List<Abonnement> pendingAbonnements = abonnementRepository.findByStatut(StatutAbonnement.EN_ATTENTE_PAIEMENT);

        for (Abonnement abonnement : pendingAbonnements) {
            // Priority 1: If we have a payment ID, verify status first
            if (abonnement.getPaiementId() != null) {
                try {
                    log.info("Vérification du statut de paiement pour l'abonnement {}", abonnement.getId());
                    PaymentResponseDTO status = paiementClient.verifierPaiement(abonnement.getPaiementId());
                    if ("SUCCESS".equals(status.getStatut())) {
                        log.info("Paiement confirmé pour l'abonnement {} via scheduler.", abonnement.getId());
                        abonnement.setStatut(StatutAbonnement.ACTIF);
                        abonnement.setDateDebut(LocalDateTime.now());
                        abonnement.setDateFin(abonnementService.getActif(abonnement.getUserId()) == null ? 
                            LocalDateTime.now().plusMonths(1) : abonnement.getDateFin()); // Simple default
                        abonnementRepository.save(abonnement);
                        continue;
                    }
                } catch (Exception e) {
                    log.debug("Le paiement {} n'est pas encore confirmé ou G6 injoignable.", abonnement.getPaiementId());
                }
            }

            // Priority 2: If no success yet and max attempts not reached, retry initiation
            if (abonnement.getNbTentativesPaiement() >= MAX_ATTEMPTS) {
                log.warn("Nombre maximum de tentatives atteint pour l'abonnement {}. Passage en ECHEC_PAIEMENT.", abonnement.getId());
                abonnement.setStatut(StatutAbonnement.ECHEC_PAIEMENT);
                abonnementRepository.save(abonnement);
                notifyFailure(abonnement, "Échec définitif du paiement après " + MAX_ATTEMPTS + " tentatives.");
                continue;
            }

            try {
                log.info("Tentative de ré-initiation de paiement #{} pour l'abonnement {}", abonnement.getNbTentativesPaiement() + 1, abonnement.getId());
                UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
                
                PaymentRequestDTO request = PaymentRequestDTO.builder()
                        .userId(abonnement.getUserId())
                        .sourceType("SUBSCRIPTION")
                        .sourceId(abonnement.getId())
                        .amount(abonnement.getPrixPaye())
                        .paymentMethod(getRandomPaymentMethod())
                        .email(user.getEmail()) // Utilisation de l'email (G3 ou local)
                        .description("Retry automatique - plan " + abonnement.getPlan().getNomPlan())
                        .build();

                PaymentResponseDTO response = paiementClient.initierPaiement(request);
                abonnement.setPaiementId(response.getTransactionId());
                log.info("Nouvelle initiation réussie pour l'abonnement {} (ID: {})", abonnement.getId(), response.getTransactionId());
                
            } catch (Exception e) {
                log.error("Échec de la ré-initiation pour l'abonnement {} : {}", abonnement.getId(), e.getMessage());
            } finally {
                abonnement.setNbTentativesPaiement(abonnement.getNbTentativesPaiement() + 1);
                abonnement.setDateDerniereTentativePaiement(LocalDateTime.now());
                abonnementRepository.save(abonnement);
            }
        }
    }

    /**
     * Tente de traiter les renouvellements en attente toutes les 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void retryPendingRenewals() {
        log.info("Lancement du scheduler de retry des renouvellements en attente...");
        List<com.serviceabonnement.entity.Renouvellement> pendingRenewals = 
            renouvellementRepository.findByStatut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE);

        for (com.serviceabonnement.entity.Renouvellement renouv : pendingRenewals) {
            // Si déjà associé à un paiement_id, c'est qu'il est en cours de callback
            if (renouv.getPaiementId() != null) continue;

            if (renouv.getNbTentatives() >= MAX_ATTEMPTS) {
                log.warn("Nombre maximum de tentatives atteint pour le renouvellement {}. Passage en ECHOUE.", renouv.getId());
                renouv.setStatut(com.serviceabonnement.enums.StatutRenouvellement.ECHOUE);
                renouvellementRepository.save(renouv);
                // Notification
                UserDTO user = fetchUserWithFallback(renouv.getAbonnement().getUserId(), renouv.getUserEmail());
                eventPublisher.publishRenouvellementEchoue(user, renouv.getAbonnement().getPlan().getNomPlan(), 
                    "Échec définitif après " + MAX_ATTEMPTS + " tentatives", renouv.getNbTentatives(), MAX_ATTEMPTS, null);
                continue;
            }

            try {
                log.info("Tentative de renouvellement #{} pour l'abonnement {}", renouv.getNbTentatives() + 1, renouv.getAbonnement().getId());
                UserDTO user = fetchUserWithFallback(renouv.getAbonnement().getUserId(), renouv.getUserEmail());
                
                PaymentRequestDTO request = PaymentRequestDTO.builder()
                        .userId(renouv.getAbonnement().getUserId())
                        .sourceType("SUBSCRIPTION")
                        .sourceId(renouv.getAbonnement().getId())
                        .amount(renouv.getPrixApplique())
                        .paymentMethod(getRandomPaymentMethod())
                        .email(user.getEmail()) // Utilisation de l'email (G3 ou local)
                        .description("Retry automatique renouvellement - plan " + renouv.getAbonnement().getPlan().getNomPlan())
                        .build();

                PaymentResponseDTO response = paiementClient.initierPaiement(request);
                renouv.setPaiementId(response.getTransactionId());
                log.info("Initiation du paiement réussie pour le renouvellement {}", renouv.getId());
                
            } catch (Exception e) {
                log.error("Nouvel échec de renouvellement pour l'abonnement {} : {}", renouv.getAbonnement().getId(), e.getMessage());
            } finally {
                renouv.setNbTentatives(renouv.getNbTentatives() + 1);
                renouv.setDateDerniereTentative(LocalDateTime.now());
                renouvellementRepository.save(renouv);
            }
        }
    }

    /**
     * Tente de traiter les remboursements en attente toutes les 5 minutes.
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void retryPendingRefunds() {
        log.info("Lancement du scheduler de retry des remboursements en attente...");
        List<Abonnement> refundingAbonnements = abonnementRepository.findByStatut(StatutAbonnement.ANNULATION_EN_COURS);

        for (Abonnement abonnement : refundingAbonnements) {
            // Si on a déjà un remboursement ID, on pourrait vérifier le statut (facultatif ici car rembourser est souvent final)
            if (abonnement.getRemboursementId() != null) {
                log.info("Remboursement déjà initié pour l'abonnement {}, en attente de traitement final.", abonnement.getId());
                continue; 
            }

            if (abonnement.getNbTentativesRemb() >= MAX_ATTEMPTS) {
                log.warn("Nombre maximum de tentatives atteint pour le remboursement de l'abonnement {}. Passage en ECHEC_REMBOURSEMENT.", abonnement.getId());
                abonnement.setStatut(StatutAbonnement.ECHEC_REMBOURSEMENT);
                abonnementRepository.save(abonnement);
                notifyRefundFailure(abonnement, "Échec définitif du remboursement après " + MAX_ATTEMPTS + " tentatives.");
                continue;
            }

            try {
                log.info("Tentative de remboursement #{} pour l'abonnement {}", abonnement.getNbTentativesRemb() + 1, abonnement.getId());
                double montant = abonnementService.calculerRemboursement(abonnement);

                RefundRequestDTO request = RefundRequestDTO.builder()
                        .transactionId(abonnement.getPaiementId())
                        .montantRemboursement(montant)
                        .motif("Retry automatique annulation")
                        .build();

                PaymentResponseDTO response = paiementClient.rembourser(request);
                
                // Si succès -> Passage en ANNULE
                log.info("Remboursement réussi pour l'abonnement {} via scheduler.", abonnement.getId());
                abonnement.setStatut(StatutAbonnement.ANNULE);
                abonnement.setRemboursementId(response.getTransactionId());
                abonnement.setDateAnnulation(LocalDateTime.now());
                
                // Notification succès (G3 first)
                UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
                eventPublisher.publishAnnulationEffectuee(user, abonnement, montant, response.getTransactionId());
                
            } catch (Exception e) {
                log.error("Échec de l'appel remboursement pour l'abonnement {} : {}", abonnement.getId(), e.getMessage());
            } finally {
                abonnement.setNbTentativesRemb(abonnement.getNbTentativesRemb() + 1);
                abonnement.setDateDerniereTentativeRemb(LocalDateTime.now());
                abonnementRepository.save(abonnement);
            }
        }
    }

    private void notifyFailure(Abonnement abonnement, String motif) {
        try {
            UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
            eventPublisher.publishEchecSouscription(user, abonnement.getPlan().getNomPlan(), motif);
        } catch (Exception e) {
            log.error("Erreur lors de la notification d'échec : {}", e.getMessage());
        }
    }

    private void notifyRefundFailure(Abonnement abonnement, String motif) {
        try {
            UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
            eventPublisher.publishAnnulationEchoue(user, abonnement.getPlan().getNomPlan(), motif);
        } catch (Exception e) {
            log.error("Erreur lors de la notification d'échec remboursement : {}", e.getMessage());
        }
    }

    private UserDTO fetchUserWithFallback(Long userId, String localEmail) {
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("G3 indisponible lors du retry pour l'utilisateur {}, utilisation de l'email local : {}", userId, localEmail);
            return UserDTO.builder()
                    .id(userId)
                    .email(localEmail)
                    .build();
        }
    }

    private String getRandomPaymentMethod() {
        return new Random().nextBoolean() ? "CARD" : "MOBILE_MONEY";
    }
}
