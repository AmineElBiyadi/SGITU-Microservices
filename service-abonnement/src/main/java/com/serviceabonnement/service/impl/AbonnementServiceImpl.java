package com.serviceabonnement.service.impl;

import com.serviceabonnement.client.PaiementClient;
import com.serviceabonnement.client.UtilisateurServiceClient;
import com.serviceabonnement.dto.AbonnementEvent;
import com.serviceabonnement.dto.external.PaymentRequestDTO;
import com.serviceabonnement.dto.external.PaymentResponseDTO;
import com.serviceabonnement.dto.external.UserDTO;
import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.enums.StatutAbonnement;
import com.serviceabonnement.enums.TypeNotification;
import com.serviceabonnement.producer.AbonnementProducer;
import com.serviceabonnement.repository.AbonnementRepository;
import com.serviceabonnement.repository.PlanAbonnementRepository;
import com.serviceabonnement.service.AbonnementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonnementServiceImpl implements AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final PlanAbonnementRepository planRepository;
    private final UtilisateurServiceClient userClient;
    private final PaiementClient paiementClient;
    private final AbonnementProducer eventProducer;

    @Override
    @Transactional
    public Abonnement souscrire(Long userId, Long planId) {
        log.info("Tentative de souscription pour l'utilisateur {} au plan {}", userId, planId);

        // 1. Vérification Utilisateur (G3)
        UserDTO user = userClient.getUserById(userId);
        if (user == null || !user.isActive()) {
            throw new RuntimeException("L'utilisateur n'existe pas ou est inactif");
        }

        // 2. Récupération du Plan
        PlanAbonnement plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan non trouvé"));

        // 3. Création de l'abonnement (Statut initial : EN_ATTENTE de paiement ou SUSPENDU avant activation)
        // Note: Dans ce flux, on va considérer qu'on crée l'abonnement en attente
        Abonnement abonnement = Abonnement.builder()
                .userId(userId)
                .plan(plan)
                .prixPaye(plan.getPrix()) // Snapshot du prix
                .statut(StatutAbonnement.SUSPENDU) // En attente de paiement
                .renouvellementAuto(true)
                .build();

        abonnement = abonnementRepository.save(abonnement);

        // 4. Initiation Paiement (G6)
        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .abonnementId(abonnement.getId())
                    .userId(userId)
                    .montant(plan.getPrix())
                    .description("Souscription plan " + plan.getNomPlan())
                    .build();

            PaymentResponseDTO paymentResponse = paiementClient.initierPaiement(paymentRequest);
            abonnement.setPaiementId(paymentResponse.getTransactionId());
            abonnementRepository.save(abonnement);

        } catch (Exception e) {
            log.error("Échec de l'initiation du paiement pour l'abonnement {}", abonnement.getId());
            eventProducer.sendSouscriptionEvent(AbonnementEvent.builder()
                    .abonnementId(abonnement.getId())
                    .userId(userId)
                    .type(TypeNotification.ECHEC_SOUSCRIPTION)
                    .motif("Erreur service paiement")
                    .build());
            throw new RuntimeException("Erreur service paiement");
        }

        return abonnement;
    }

    @Override
    public Abonnement getAbonnementById(Long id) {
        return abonnementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé"));
    }

    @Override
    public List<Abonnement> getAbonnementsByUtilisateur(Long userId) {
        return abonnementRepository.findByUserId(userId);
    }

    @Override
    public Abonnement getActif(Long userId) {
        return abonnementRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatut() == StatutAbonnement.ACTIF)
                .findFirst()
                .orElse(null);
    }

    private final com.serviceabonnement.repository.DesactivationRepository desactivationRepository;
    private final com.serviceabonnement.repository.RenouvellementRepository renouvellementRepository;

    @Override
    @Transactional
    public void forcerRenouvellement(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        // Même logique que renouveler mais marqué comme MANUEL
        PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .montant(plan.getPrix())
                .description("Renouvellement forcé par admin - plan " + plan.getNomPlan())
                .build();

        PaymentResponseDTO response = paiementClient.initierPaiement(paymentRequest);
        
        com.serviceabonnement.entity.Renouvellement renouvellement = com.serviceabonnement.entity.Renouvellement.builder()
                .abonnement(abonnement)
                .paiementId(response.getTransactionId())
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.MANUEL)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
        renouvellementRepository.save(renouvellement);

        eventProducer.sendRenouvellementEvent(AbonnementEvent.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .type(TypeNotification.RENOUVELLEMENT_EFFECTUE)
                .motif("Renouvellement manuel par admin")
                .build());
    }

    @Override
    @Transactional
    public void desactiver(Long abonnementId, int jours) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        // Vérification des règles de désactivation du plan
        if (jours > plan.getMaxPeriodeDesactivation()) {
            throw new RuntimeException("Durée de désactivation trop longue");
        }

        // Logique de pause
        abonnement.setStatut(StatutAbonnement.SUSPENDU);
        // On repousse la date de fin
        abonnement.setDateFin(abonnement.getDateFin().plusDays(jours));
        
        abonnementRepository.save(abonnement);

        // Historisation
        com.serviceabonnement.entity.Desactivation desactivation = com.serviceabonnement.entity.Desactivation.builder()
                .abonnement(abonnement)
                .dateDebutDesactivation(LocalDateTime.now())
                .dateFinDesactivation(LocalDateTime.now().plusDays(jours))
                .build();
        desactivationRepository.save(desactivation);

        eventProducer.sendDesactivationEvent(AbonnementEvent.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .type(TypeNotification.DESACTIVATION_EFFECTUEE)
                .build());
    }

    @Override
    @Transactional
    public void suspendre(Long abonnementId, String motif) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        abonnement.setStatut(StatutAbonnement.SUSPENDU);
        abonnementRepository.save(abonnement);

        eventProducer.sendSuspensionEvent(AbonnementEvent.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .type(TypeNotification.SUSPENSION_EFFECTUEE)
                .motif(motif)
                .build());
    }

    @Override
    @Transactional
    public void demanderAnnulation(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        
        Double montantRemboursement = calculerRemboursement(abonnement);
        
        abonnement.setStatut(StatutAbonnement.ANNULATION_EN_COURS);
        abonnement.setDateDemandeAnnulation(LocalDateTime.now());
        abonnementRepository.save(abonnement);

        // Appel G6 pour le remboursement
        com.serviceabonnement.dto.external.RefundRequestDTO refundRequest = com.serviceabonnement.dto.external.RefundRequestDTO.builder()
                .transactionId(abonnement.getPaiementId())
                .montantRemboursement(montantRemboursement)
                .motif("Annulation utilisateur")
                .build();

        paiementClient.rembourser(refundRequest);

        eventProducer.sendAnnulationEvent(AbonnementEvent.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .type(TypeNotification.ANNULATION_EFFECTUEE)
                .build());
    }

    private Double calculerRemboursement(Abonnement abonnement) {
        if (abonnement.getDateDebut() == null || abonnement.getDateFin() == null) return 0.0;
        
        java.time.Duration totalDuration = java.time.Duration.between(abonnement.getDateDebut(), abonnement.getDateFin());
        java.time.Duration remainingDuration = java.time.Duration.between(LocalDateTime.now(), abonnement.getDateFin());
        
        long totalDays = totalDuration.toDays();
        long remainingDays = remainingDuration.toDays();
        
        if (totalDays <= 0 || remainingDays <= 0) return 0.0;
        
        return (abonnement.getPrixPaye() / totalDays) * remainingDays;
    }

    @Override
    @Transactional
    public Abonnement renouveler(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        log.info("Renouvellement pour l'abonnement {}", abonnementId);

        // Initiation Paiement
        PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .montant(plan.getPrix())
                .description("Renouvellement plan " + plan.getNomPlan())
                .build();

        PaymentResponseDTO response = paiementClient.initierPaiement(paymentRequest);
        
        // On crée une trace de renouvellement
        com.serviceabonnement.entity.Renouvellement renouvellement = com.serviceabonnement.entity.Renouvellement.builder()
                .abonnement(abonnement)
                .paiementId(response.getTransactionId())
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.AUTOMATIQUE)
                .build();
        
        // Note: Le statut passera à ACTIF lors du callback de paiement (Phase 6)
        
        eventProducer.sendRenouvellementEvent(AbonnementEvent.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .type(TypeNotification.RENOUVELLEMENT_EFFECTUE)
                .build());

        return abonnement;
    }

    @Override
    @Transactional
    public void forcerAnnulation(Long abonnementId, String motif) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        abonnement.setStatut(StatutAbonnement.ANNULE);
        abonnement.setDateAnnulation(LocalDateTime.now());
        abonnementRepository.save(abonnement);

        eventProducer.sendAnnulationEvent(AbonnementEvent.builder()
                .abonnementId(abonnementId)
                .userId(abonnement.getUserId())
                .type(TypeNotification.ANNULATION_ADMIN_EFFECTUE)
                .motif(motif)
                .build());
    }

    @Override
    @Transactional
    public void confirmerPaiement(String transactionId) {
        Abonnement abonnement = abonnementRepository.findByPaiementId(transactionId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé pour la transaction: " + transactionId));
        
        abonnement.setStatut(StatutAbonnement.ACTIF);
        abonnement.setDateDebut(LocalDateTime.now());
        
        // Calcul de la date de fin selon la durée de l'offre
        LocalDateTime dateFin;
        switch (abonnement.getPlan().getDuree()) {
            case HEBDOMADAIRE -> dateFin = LocalDateTime.now().plusWeeks(1);
            case MENSUEL -> dateFin = LocalDateTime.now().plusMonths(1);
            case TRIMESTRIEL -> dateFin = LocalDateTime.now().plusMonths(3);
            case ANNUEL -> dateFin = LocalDateTime.now().plusYears(1);
            default -> dateFin = LocalDateTime.now().plusMonths(1);
        }
        abonnement.setDateFin(dateFin);
        
        abonnementRepository.save(abonnement);
        log.info("Abonnement {} activé suite à confirmation paiement", abonnement.getId());

        eventProducer.sendSouscriptionEvent(AbonnementEvent.builder()
                .abonnementId(abonnement.getId())
                .userId(abonnement.getUserId())
                .type(TypeNotification.CONFIRMATION_SOUSCRIPTION)
                .build());
    }

    @Override
    @Transactional
    public void confirmerRemboursement(String transactionId) {
        // Le remboursement peut être lié soit au paiementId original soit à un remboursementId spécifique
        Abonnement abonnement = abonnementRepository.findByPaiementId(transactionId)
                .orElseGet(() -> abonnementRepository.findByRemboursementId(transactionId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé pour le remboursement: " + transactionId)));
        
        abonnement.setStatut(StatutAbonnement.ANNULE);
        abonnement.setDateAnnulation(LocalDateTime.now());
        abonnementRepository.save(abonnement);
        
        log.info("Abonnement {} annulé suite à confirmation remboursement", abonnement.getId());

        eventProducer.sendAnnulationEvent(AbonnementEvent.builder()
                .abonnementId(abonnement.getId())
                .userId(abonnement.getUserId())
                .type(TypeNotification.REUSSITE_REMBOURSEMENT)
                .build());
    }
}
