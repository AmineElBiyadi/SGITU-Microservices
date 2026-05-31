package com.serviceabonnement.service.impl;

import com.serviceabonnement.client.AnalyseClient;
import com.serviceabonnement.client.PaiementClient;
import com.serviceabonnement.client.UtilisateurServiceClient;
import com.serviceabonnement.dto.external.PaymentRequestDTO;
import com.serviceabonnement.dto.external.PaymentResponseDTO;
import com.serviceabonnement.dto.external.UserDTO;
import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.enums.StatutAbonnement;
import com.serviceabonnement.exception.AbonnementNotFoundException;
import com.serviceabonnement.exception.AbonnementStatutInvalideException;
import com.serviceabonnement.exception.BaseException;
import com.serviceabonnement.exception.ConflictException;
import com.serviceabonnement.exception.PaiementServiceException;
import com.serviceabonnement.exception.PlanNotFoundException;
import com.serviceabonnement.exception.RegleMetierException;
import com.serviceabonnement.exception.UtilisateurNotFoundException;
import com.serviceabonnement.producer.SubscriptionEventPublisher;
import com.serviceabonnement.repository.AbonnementRepository;
import com.serviceabonnement.repository.AnalytiqueTraceRepository;
import com.serviceabonnement.repository.DesactivationRepository;
import com.serviceabonnement.repository.PlanAbonnementRepository;
import com.serviceabonnement.repository.RenouvellementRepository;
import com.serviceabonnement.repository.SuspensionAdminRepository;
import com.serviceabonnement.service.AbonnementService;
import com.serviceabonnement.entity.SuspensionAdmin;
import com.serviceabonnement.entity.AnalytiqueTrace;
import com.serviceabonnement.entity.Desactivation;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.List;
import com.serviceabonnement.entity.Renouvellement;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbonnementServiceImpl implements AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final PlanAbonnementRepository planRepository;
    private final UtilisateurServiceClient userClient;
    private final PaiementClient paiementClient;
    private final AnalyseClient analyseClient;
    private final SubscriptionEventPublisher eventPublisher;
    private final AnalytiqueTraceRepository analytiqueTraceRepository;
    private final DesactivationRepository desactivationRepository;
    private final RenouvellementRepository renouvellementRepository;
    private final SuspensionAdminRepository suspensionAdminRepository;

    @Autowired
    @Lazy
    private AbonnementServiceImpl self;

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "souscrireFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public Abonnement souscrire(Long userId, Long planId, String email) {
        log.info("Tentative de souscription pour l'utilisateur {} au plan {}", userId, planId);

        if (email == null) email = "";

        // 1. Vérification Utilisateur via G3
        UserDTO user;
        try {
            user = userClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Impossible de vérifier l'utilisateur {} via G3: {}", userId, e.getMessage());
            user = null;
        }

        // FALLBACK G3 : Si G3 est injoignable ou en erreur, on accepte si on a au moins l'email du JWT
        if (user == null && (email == null || email.isEmpty())) {
            log.error("Échec de vérification utilisateur {}: G3 indisponible et aucun email dans le JWT", userId);
            throw new UtilisateurNotFoundException(userId);
        }

        if (user != null && !user.isActive()) {
            throw new UtilisateurNotFoundException(userId);
        }

        // 2. Récupération du Plan
        PlanAbonnement plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        // 3. Vérification d'éligibilité via les rôles du JWT (from SecurityContext)
        List<String> userRoles = extractUserRoles();
        if (!isEligibleForPlan(userRoles, plan.getCategorie().name())) {
            log.error("Utilisateur {} non éligible pour le plan {}", userId, plan.getCategorie().name());
            throw new RegleMetierException("Utilisateur non eligible pour le plan " + plan.getCategorie().name() + ".");
        }

        // 4. Calcul du prix (plan-change vs nouvelle souscription)
        java.util.Optional<Abonnement> abonnementMemeTransport = abonnementRepository
                .findByUserIdAndStatut(userId, StatutAbonnement.ACTIF).stream()
                .filter(a -> a.getPlan().getTransportType() == plan.getTransportType())
                .findFirst();

        double prixAPayer = plan.getPrix();

        if (abonnementMemeTransport.isPresent()) {
            Abonnement abonnementActif = abonnementMemeTransport.get();
            validerTransitionAbonnement(abonnementActif, plan);
            double valeurRestante = calculerRemboursement(abonnementActif);
            if (valeurRestante > 0) {
                prixAPayer = plan.getPrix() >= valeurRestante
                        ? plan.getPrix() - valeurRestante   // Upgrade / Switch
                        : 0.0;                              // Downgrade (remboursement à l'activation)
            }
        }

        // 5. IDEMPOTENCE : Vérifier si une souscription en attente existe déjà (Retry Resilience4j)
        Abonnement abonnement = abonnementRepository.findByUserIdAndPlanIdPlanAndStatut(userId, planId, StatutAbonnement.EN_ATTENTE_PAIEMENT)
                .filter(a -> a.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5)))
                .orElse(null);

        if (abonnement == null) {
            abonnement = Abonnement.builder()
                    .userId(userId)
                    .plan(plan)
                    .userEmail(email) // Stockage local pour résilience
                    .prixPaye(prixAPayer)
                    .statut(StatutAbonnement.EN_ATTENTE_PAIEMENT)
                    .renouvellementAuto(true)
                    .build();
            abonnement = abonnementRepository.save(abonnement);
            sendToAnalyse(abonnement, "SOUSCRIPTION_INITIALE", prixAPayer);
        } else {
            log.info("Idempotence : Souscription en attente déjà existante (ID: {}), réutilisation", abonnement.getId());
        }

        // Récupération du téléphone depuis le user déjà chargé (G3 peut être null en cas de panne)
        String phone = (user != null && user.getProfile() != null) ? user.getProfile().getPhone() : null;
        if (email.isEmpty() && user != null && user.getEmail() != null) email = user.getEmail();

        // 6. Initiation Paiement (G6)
        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .userId(userId)
                    .sourceType("SUBSCRIPTION")
                    .sourceId(abonnement.getId())
                    .amount(prixAPayer)
                    .paymentMethod(getRandomPaymentMethod())
                    .email(email)
                    .description("Souscription plan " + plan.getNomPlan())
                    .build();
            PaymentResponseDTO paymentResponse = paiementClient.initierPaiement(paymentRequest);
            abonnement.setPaiementId(paymentResponse.getTransactionId());
            abonnementRepository.save(abonnement);
        } catch (Exception e) {
            log.warn("Échec de l'initiation du paiement immédiat pour l'abonnement {}. L'abonnement reste en attente et sera traité par le scheduler. Erreur: {}", abonnement.getId(), e.getMessage());
            eventPublisher.publishEchecSouscription(userId, email, phone, plan.getNomPlan(), "Service paiement indisponible (traitement différé)");
            // On ne jette PAS d'exception ici pour permettre au client de recevoir le 201 Created
            // avec le statut EN_ATTENTE_PAIEMENT. Le scheduler prendra le relais.
        }

        return abonnement;
    }

    @Override
    public Abonnement getAbonnementById(Long id) {
        return abonnementRepository.findById(id)
                .orElseThrow(() -> new AbonnementNotFoundException("Abonnement introuvable avec l'ID : " + id));
    }

    @Override
    public List<Abonnement> getAbonnementsByUtilisateur(Long userId) {
        return abonnementRepository.findByUserId(userId);
    }

    @Override
    public Page<Abonnement> getFullHistory(Long userId, Pageable pageable) {
        return abonnementRepository.findByUserId(userId, pageable);
    }

    @Override
    public Abonnement getActif(Long userId) {
        return abonnementRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatut() == StatutAbonnement.ACTIF)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Renouvellement> getHistoriquePaiements(Long abonnementId) {
        return renouvellementRepository.findByAbonnementId(abonnementId);
    }

    @Override
    @Transactional
    public Abonnement toggleAutoRenouvellement(Long abonnementId, boolean enable) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        abonnement.setRenouvellementAuto(enable);
        return abonnementRepository.save(abonnement);
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "renouvellementFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public void forcerRenouvellement(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        log.info("Renouvellement forcé par admin pour l'abonnement {}", abonnementId);

        UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
        
        // 1. IDEMPOTENCE : Enregistrement de l'intention en base AVANT l'appel externe
        Renouvellement renouvellement = renouvellementRepository
                .findFirstByAbonnementIdAndStatutOrderByDateRenouvellementDesc(abonnementId, com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .filter(r -> r.getDateRenouvellement().isAfter(LocalDateTime.now().minusMinutes(5)))
                .orElse(null);

        if (renouvellement == null) {
            renouvellement = Renouvellement.builder()
                .abonnement(abonnement)
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.MANUEL)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
            renouvellement = renouvellementRepository.save(renouvellement);
        }

        // 2. Appel G6
        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .userId(abonnement.getUserId())
                    .sourceType("SUBSCRIPTION")
                    .sourceId(abonnementId)
                    .amount(plan.getPrix())
                    .paymentMethod(getRandomPaymentMethod())
                    .email(user.getEmail())
                    .description("Renouvellement forcé par admin - plan " + plan.getNomPlan())
                    .build();
            PaymentResponseDTO response = paiementClient.initierPaiement(paymentRequest);
            renouvellement.setPaiementId(response.getTransactionId());
            renouvellementRepository.save(renouvellement);
        } catch (Exception e) {
            log.warn("Échec de l'initiation du paiement (forcé) pour l'abonnement {}. Sera traité par le scheduler. Erreur: {}", abonnementId, e.getMessage());
        }

        eventPublisher.publishRenouvellementEffectue(user, abonnement, "MANUEL");
    }

    @Override
    @Bulkhead(name = "userService")
    public void desactiver(Long abonnementId, int jours) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        if (abonnement.getStatut() != StatutAbonnement.ACTIF) {
            throw new RegleMetierException("L'abonnement doit être ACTIF pour pouvoir être désactivé.");
        }

        // Vérification des règles de désactivation du plan
        if (jours > plan.getMaxPeriodeDesactivation()) {
            throw new RegleMetierException(
                    "Durée de désactivation " + jours + " jours dépasse le maximum autorisé ("
                            + plan.getMaxPeriodeDesactivation() + " jours)");
        }

        List<Desactivation> desactivations = desactivationRepository.findByAbonnementId(abonnementId);
        int nbUsees = desactivations.size();
        if (nbUsees >= plan.getMaxDesactivation()) {
            throw new RegleMetierException("Nombre maximum de désactivations atteint (" + plan.getMaxDesactivation() + ").");
        }

        if (!desactivations.isEmpty()) {
            Desactivation lastDesactivation = desactivations.stream()
                    .max(java.util.Comparator.comparing(Desactivation::getDateFinDesactivation))
                    .get();
            
            long daysSinceLast = java.time.temporal.ChronoUnit.DAYS.between(
                    lastDesactivation.getDateFinDesactivation(), LocalDateTime.now());
            
            if (daysSinceLast < plan.getMinJoursEntreDesactivation()) {
                throw new RegleMetierException("Délai minimum entre deux désactivations non respecté (il faut attendre " 
                        + plan.getMinJoursEntreDesactivation() + " jours, actuellement " + daysSinceLast + " jours écoulés).");
            }
        }

        // Logique de pause
        abonnement.setStatut(StatutAbonnement.DESACTIVE);
        // On repousse la date de fin
        abonnement.setDateFin(abonnement.getDateFin().plusDays(jours));

        abonnementRepository.save(abonnement);

        // Historisation
        Desactivation desactivation = Desactivation.builder()
                .abonnement(abonnement)
                .dateDebutDesactivation(LocalDateTime.now())
                .dateFinDesactivation(LocalDateTime.now().plusDays(jours))
                .build();
        desactivationRepository.save(desactivation);

        eventPublisher.publishDesactivationEffectuee(
                fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail()),
                plan.getNomPlan(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(jours),
                nbUsees + 1,
                plan.getMaxDesactivation()
        );
    }

    @Override
    @Bulkhead(name = "userService")
    public void suspendre(Long abonnementId, String motif, LocalDateTime dateFinSuspension) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        if (abonnement.getStatut() == StatutAbonnement.ANNULE ||
                abonnement.getStatut() == StatutAbonnement.SUSPENDU) {
            throw new AbonnementStatutInvalideException(
                    "Impossible de suspendre un abonnement au statut : " + abonnement.getStatut());
        }
        abonnement.setStatut(StatutAbonnement.SUSPENDU);
        abonnementRepository.save(abonnement);

        String adminId = "ROLE_ADMIN_G2";
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            adminId = auth.getName();
        }

        SuspensionAdmin suspensionAdmin = SuspensionAdmin.builder()
                .abonnement(abonnement)
                .adminId(adminId)
                .dateDebutSuspension(LocalDateTime.now())
                .dateFinSuspension(dateFinSuspension)
                .motif(motif)
                .build();
        suspensionAdminRepository.save(suspensionAdmin);

        eventPublisher.publishSuspensionEffectuee(
                fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail()),
                abonnement.getPlan().getNomPlan(),
                adminId,
                motif,
                LocalDateTime.now());
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "annulationFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public void demanderAnnulation(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        if (abonnement.getStatut() == StatutAbonnement.ANNULE ||
                abonnement.getStatut() == StatutAbonnement.ANNULATION_EN_COURS ||
                abonnement.getStatut() == StatutAbonnement.EXPIRE) {
            throw new AbonnementStatutInvalideException(
                "L'abonnement est déjà annulé ou en cours d'annulation : " + abonnement.getStatut());
        }

        // Calcul des jours restants avant la fin de l'abonnement
        long joursRestants = 0;
        if (abonnement.getDateFin() != null) {
            joursRestants = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), abonnement.getDateFin());
        }
        
        Double montantRemboursement = 0.0;

        if (joursRestants >= 3 && abonnement.getDateFin() != null) {
            montantRemboursement = calculerRemboursement(abonnement);
        } else {
            log.info(
                    "Annulation de l'abonnement {} sans remboursement (pas de date de fin ou < 3 jours)",
                    abonnementId);
        }

        if (montantRemboursement > 0) {
            abonnement.setStatut(StatutAbonnement.ANNULATION_EN_COURS);
        } else {
            abonnement.setStatut(StatutAbonnement.ANNULE);
            abonnement.setDateAnnulation(LocalDateTime.now());
        }
        
        abonnement.setDateDemandeAnnulation(LocalDateTime.now());
        abonnementRepository.save(abonnement);

        // 2. Appel G6 pour le remboursement uniquement si le montant est supérieur à 0
        if (montantRemboursement > 0) {
            try {
                com.serviceabonnement.dto.external.RefundRequestDTO refundRequest = com.serviceabonnement.dto.external.RefundRequestDTO.builder()
                        .transactionId(abonnement.getPaiementId())
                        .montantRemboursement(montantRemboursement)
                        .motif("Annulation utilisateur - Franchise de 3 jours respectée")
                        .build();
                paiementClient.rembourser(refundRequest);
            } catch (Exception e) {
                log.warn("Impossible d'effectuer le remboursement immmédiat pour l'abonnement {}. Sera traité par le scheduler. Erreur: {}", abonnementId, e.getMessage());
                // On ne jette pas d'exception pour que l'utilisateur reçoive le message de succès différé
            }
        }

        if (abonnement.getStatut() == StatutAbonnement.ANNULE) {
            try {
                UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
                eventPublisher.publishAnnulationEffectuee(user, abonnement, montantRemboursement, abonnement.getPaiementId());
            } catch (Exception e) {
                log.error("Erreur lors de la notification d'annulation pour {}: {}", abonnementId, e.getMessage());
            }
        }
    }

    @Override
    public Double calculerRemboursement(Abonnement abonnement) {
        if (abonnement.getDateDebut() == null || abonnement.getDateFin() == null)
            return 0.0;

        java.time.Duration totalDuration = java.time.Duration.between(abonnement.getDateDebut(),
                abonnement.getDateFin());
        java.time.Duration remainingDuration = java.time.Duration.between(LocalDateTime.now(), abonnement.getDateFin());

        long totalDays = totalDuration.toDays();
        long remainingDays = remainingDuration.toDays();

        if (totalDays <= 0 || remainingDays <= 0)
            return 0.0;

        return (abonnement.getPrixPaye() / totalDays) * remainingDays;
    }

    private LocalDateTime calculerDateFin(com.serviceabonnement.enums.DureeOffre duree) {
        return calculerDateFin(duree, LocalDateTime.now());
    }

    private LocalDateTime calculerDateFin(com.serviceabonnement.enums.DureeOffre duree, LocalDateTime startDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
        return switch (duree) {
            case HEBDOMADAIRE -> startDate.plusWeeks(1);
            case MENSUEL -> startDate.plusMonths(1);
            case TRIMESTRIEL -> startDate.plusMonths(3);
            case ANNUEL -> startDate.plusYears(1);
        };
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "renouvellementFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public Abonnement renouvelerManuel(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        log.info("Renouvellement manuel demandé pour l'abonnement {}", abonnementId);

        UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());

        // 1. IDEMPOTENCE
        Renouvellement renouvellement = renouvellementRepository
                .findFirstByAbonnementIdAndStatutOrderByDateRenouvellementDesc(abonnementId, com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .filter(r -> r.getDateRenouvellement().isAfter(LocalDateTime.now().minusMinutes(5)))
                .orElse(null);

        if (renouvellement == null) {
            renouvellement = Renouvellement.builder()
                .abonnement(abonnement)
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.MANUEL)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
            renouvellement = renouvellementRepository.save(renouvellement);
        }

        // 2. Appel G6
        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .userId(abonnement.getUserId())
                    .sourceType("SUBSCRIPTION")
                    .sourceId(abonnementId)
                    .amount(plan.getPrix())
                    .paymentMethod(getRandomPaymentMethod())
                    .email(user.getEmail())
                    .description("Renouvellement manuel - plan " + plan.getNomPlan())
                    .build();
            PaymentResponseDTO response = paiementClient.initierPaiement(paymentRequest);
            renouvellement.setPaiementId(response.getTransactionId());
            renouvellementRepository.save(renouvellement);
        } catch (Exception e) {
            log.warn("Échec de l'initiation du paiement manuel pour l'abonnement {}. Traitement différé. Erreur: {}", abonnementId, e.getMessage());
        }

        eventPublisher.publishRenouvellementEffectue(user, abonnement, "MANUEL");

        return abonnement;
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "renouvellementFallback")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    public Abonnement renouveler(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        log.info("Renouvellement automatique pour l'abonnement {}", abonnementId);

        UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());

        // 1. IDEMPOTENCE
        Renouvellement renouvellement = renouvellementRepository
                .findFirstByAbonnementIdAndStatutOrderByDateRenouvellementDesc(abonnementId, com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .filter(r -> r.getDateRenouvellement().isAfter(LocalDateTime.now().minusMinutes(5)))
                .orElse(null);

        if (renouvellement == null) {
            renouvellement = Renouvellement.builder()
                .abonnement(abonnement)
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .userEmail(user.getEmail()) // Stockage local
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.AUTOMATIQUE)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
            renouvellement = renouvellementRepository.save(renouvellement);
        }

        // 2. Appel G6
        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .userId(abonnement.getUserId())
                    .sourceType("SUBSCRIPTION")
                    .sourceId(abonnementId)
                    .amount(plan.getPrix())
                    .paymentMethod(getRandomPaymentMethod())
                    .email(user.getEmail())
                    .description("Renouvellement automatique plan " + plan.getNomPlan())
                    .build();
            PaymentResponseDTO response = paiementClient.initierPaiement(paymentRequest);
            renouvellement.setPaiementId(response.getTransactionId());
            renouvellementRepository.save(renouvellement);
        } catch (Exception e) {
            log.warn("Échec de l'initiation du paiement automatique pour l'abonnement {}. Traitement différé. Erreur: {}", abonnementId, e.getMessage());
        }
        
        eventPublisher.publishRenouvellementEffectue(user, abonnement, "AUTOMATIQUE");

        return abonnement;
    }

    @Override
    @Transactional
    public void forcerAnnulation(Long abonnementId, String motif) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        abonnement.setStatut(StatutAbonnement.ANNULE);
        abonnement.setDateAnnulation(LocalDateTime.now());
        abonnementRepository.save(abonnement);

        eventPublisher.publishAnnulationEffectuee(
                fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail()),
                abonnement,
                0.0, // Pas de remboursement automatique sur annulation admin forcée ici
                null);
    }

    @Override
    @Bulkhead(name = "paymentService")
    public void confirmerPaiement(com.serviceabonnement.dto.external.PaymentCallbackDTO callback) {
        String token = callback.getTransactionToken();

        // --- Branch 1: New subscription payment ---
        java.util.Optional<Abonnement> abonnementOpt = abonnementRepository.findByPaiementId(token);
        if (abonnementOpt.isPresent()) {
            Abonnement abonnement = abonnementOpt.get();

            // Idempotency guard — ignore duplicate webhook
            if (abonnement.getStatut() == StatutAbonnement.ACTIF) {
                log.warn("Webhook reçu pour un abonnement déjà actif {}. Ignoré (idempotence).", abonnement.getId());
                return;
            }

            if ("SUCCESS".equals(callback.getStatus())) {
                abonnement.setStatut(StatutAbonnement.ACTIF);
                abonnement.setDateDebut(LocalDateTime.now());
                abonnement.setDateFin(calculerDateFin(abonnement.getPlan().getDuree()));

                // Isolated REQUIRES_NEW transaction — refund failure for old plan
                // must never roll back the new plan's activation.
                self.annulerAnciensAbonnementsActifsEnConflit(abonnement);

                log.info("Abonnement {} activé avec succès (Token: {})", abonnement.getId(), token);
                sendToAnalyse(abonnement, "SOUSCRIPTION_CONFIRMEE", abonnement.getPrixPaye());
                eventPublisher.publishConfirmationSouscription(
                        fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail()), abonnement);
            } else {
                abonnement.setStatut(StatutAbonnement.ECHEC_PAIEMENT);
                log.warn("Le paiement a échoué pour l'abonnement {} : {}", abonnement.getId(), callback.getMessage());
            }

            abonnementRepository.save(abonnement);
            return;
        }

        // --- Branch 2: Renewal payment ---
        // Token belongs to a Renouvellement, not a new subscription.
        Renouvellement renouvellement = renouvellementRepository.findByPaiementId(token)
                .orElseThrow(() -> new AbonnementNotFoundException(
                        "Aucun abonnement ou renouvellement associé au token " + token));

        // Idempotency guard for renewals
        if (renouvellement.getStatut() == com.serviceabonnement.enums.StatutRenouvellement.SUCCES) {
            log.warn("Webhook reçu pour un renouvellement déjà traité {}. Ignoré (idempotence).", renouvellement.getId());
            return;
        }

        Abonnement abonnement = renouvellement.getAbonnement();
        UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
        
        if ("SUCCESS".equals(callback.getStatus())) {
            abonnement.setDateFin(calculerDateFin(abonnement.getPlan().getDuree(), abonnement.getDateFin()));
            if (abonnement.getStatut() != StatutAbonnement.SUSPENDU) {
                abonnement.setStatut(StatutAbonnement.ACTIF);
            }
            renouvellement.setStatut(com.serviceabonnement.enums.StatutRenouvellement.SUCCES);
            abonnementRepository.save(abonnement);
            renouvellementRepository.save(renouvellement);
            log.info("Renouvellement {} confirmé. Nouvelle dateFin: {}", renouvellement.getId(), abonnement.getDateFin());
            eventPublisher.publishRenouvellementEffectue(
                    user,
                    abonnement,
                    renouvellement.getTypeRenouvellement().name());
        } else {
            renouvellement.setStatut(com.serviceabonnement.enums.StatutRenouvellement.ECHOUE);
            renouvellementRepository.save(renouvellement);
            log.warn("Paiement de renouvellement échoué pour {} : {}", renouvellement.getId(), callback.getMessage());
        }
    }

    @Override
    @Bulkhead(name = "paymentService")
    public void confirmerRemboursement(com.serviceabonnement.dto.external.RefundCallbackDTO callback) {
        Abonnement abonnement = abonnementRepository.findByPaiementId(callback.getTransactionId())
                .orElseGet(() -> abonnementRepository.findByRemboursementId(callback.getTransactionId())
                        .orElseThrow(() -> new AbonnementNotFoundException(
                                "Aucun abonnement associé à la transaction " + callback.getTransactionId())));

        UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());

        switch (callback.getStatut()) {
            case "REMBOURSE" -> {
                abonnement.setStatut(StatutAbonnement.ANNULE);
                abonnement.setDateAnnulation(LocalDateTime.now());
                log.info("Abonnement {} annulé avec succès (Remboursement effectué)", abonnement.getId());
                sendToAnalyse(abonnement, "ANNULATION_CONFIRMEE", 0.0);
                eventPublisher.publishAnnulationEffectuee(user, abonnement,
                        callback.getMontantRembourse(), callback.getTransactionId());
            }
            case "ECHEC_REMBOURSEMENT" -> {
                abonnement.setStatut(StatutAbonnement.ECHEC_REMBOURSEMENT);
                log.error("Échec définitif du remboursement pour l'abonnement {} : {}", abonnement.getId(),
                        callback.getMotif());
                // Ici on pourrait notifier un administrateur
            }
            case "EN_COURS" -> {
                abonnement.setStatut(StatutAbonnement.ANNULATION_EN_COURS);
                log.info("Remboursement en cours (tentative) pour l'abonnement {}", abonnement.getId());
            }
        }

        abonnementRepository.save(abonnement);
    }

    @Override
    public com.serviceabonnement.dto.external.ActiveSubscriptionResponseDTO verifierAbonnementActif(Long userId) {
        Abonnement actif = getActif(userId);

        if (actif != null) {
            return com.serviceabonnement.dto.external.ActiveSubscriptionResponseDTO.builder()
                    .aUnAbonnementActif(true)
                    .typePlan(actif.getPlan().getDuree().toString())
                    .dateExpiration(actif.getDateFin().toLocalDate().toString())
                    .build();
        }

        return com.serviceabonnement.dto.external.ActiveSubscriptionResponseDTO.builder()
                .aUnAbonnementActif(false)
                .typePlan(null)
                .dateExpiration(null)
                .build();
    }

    private void sendToAnalyse(Abonnement abonnement, String action, Double amount) {
        try {
            // 1. Vérification Admin (G3) - On ne suit pas les admins
            UserDTO user = fetchUserWithFallback(abonnement.getUserId(), abonnement.getUserEmail());
            if (user != null && user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN_G2")) {
                log.info("Analyse ignorée pour l'utilisateur admin_g2 {}", abonnement.getUserId());
                return;
            }

            // 2. Formatage des données
            String planType = mapPlanType(abonnement.getPlan().getDuree(), abonnement.getPlan().getCategorie());
            String formattedAction = mapAction(action);
            String timestamp = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"))
                    .format(java.time.format.DateTimeFormatter.ISO_INSTANT);

            // 3. Sauvegarde en base pour le batching (G12)
            AnalytiqueTrace trace = AnalytiqueTrace.builder()
                    .timestamp(timestamp)
                    .userId("USR-" + abonnement.getUserId())
                    .action(formattedAction)
                    .planType(planType)
                    .build();

            analytiqueTraceRepository.save(trace);
            log.info("Trace d'analyse collectée pour l'utilisateur {} - action: {}", trace.getUserId(),
                    formattedAction);

        } catch (Exception e) {
            log.error("Échec de la collecte de la trace d'analyse: {}", e.getMessage());
        }
    }

    private String mapPlanType(com.serviceabonnement.enums.DureeOffre duree,
            com.serviceabonnement.enums.CategorieAbonnement categorie) {
        String d = switch (duree) {
            case HEBDOMADAIRE -> "WEEKLY";
            case MENSUEL -> "MONTHLY";
            case TRIMESTRIEL -> "QUARTERLY";
            case ANNUEL -> "YEARLY";
        };
        String c = switch (categorie) {
            case ROLE_STUDENT -> "STUDENT";
            case ROLE_PASSENGER -> "PASSENGER";
        };
        return d + "_" + c;
    }

    private String mapAction(String action) {
        return switch (action) {
            case "SOUSCRIPTION_INITIALE" -> "created";
            case "SOUSCRIPTION_CONFIRMEE" -> "activated";
            case "ANNULATION_CONFIRMEE" -> "cancelled";
            default -> action.toLowerCase();
        };
    }

    private void validerTransitionAbonnement(Abonnement abonnementActif, PlanAbonnement nouveauPlan) {
        PlanAbonnement planActuel = abonnementActif.getPlan();

        if (planActuel.getIdPlan().equals(nouveauPlan.getIdPlan())) {
            throw new ConflictException("L'utilisateur possede deja un abonnement actif sur ce plan.");
        }

        boolean memeTransport = planActuel.getTransportType() == nouveauPlan.getTransportType();
        boolean memeCategorie = planActuel.getCategorie() == nouveauPlan.getCategorie();
        boolean memeDuree = planActuel.getDuree() == nouveauPlan.getDuree();

        boolean changementDureeMemeCategorie = memeTransport && memeCategorie && !memeDuree;
        boolean changementCategorieMemeOffre = memeTransport && !memeCategorie && memeDuree;

        if (!changementDureeMemeCategorie && !changementCategorieMemeOffre) {
            throw new RegleMetierException(
                    "Changement refuse : il faut garder le meme transport et changer uniquement la duree ou la categorie.");
        }
    }

    /**
     * Bug 2 Fix: @Transactional(REQUIRES_NEW) ensures this runs in its own independent
     * transaction. A failure in the G6 refund call (downgrade scenario) will NOT roll back
     * the outer confirmerPaiement transaction that activated the new subscription.
     *
     * PLAN-CHANGE CANCELLATION ONLY — NOT for regular user cancellations.
     * Refund logic:
     *   - Upgrade (newPrice >= oldRemaining): user already paid reduced price → no refund here.
     *   - Downgrade (newPrice < oldRemaining): user paid 0 → refund = oldRemaining - newPrice.
     *   - Regular Cancellation: handled separately in demanderAnnulation().
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void annulerAnciensAbonnementsActifsEnConflit(Abonnement nouvelAbonnement) {
        List<Abonnement> abonnementsActifs = abonnementRepository.findByUserIdAndStatut(
                nouvelAbonnement.getUserId(),
                StatutAbonnement.ACTIF
        );

        abonnementsActifs.stream()
                .filter(abonnement -> !abonnement.getId().equals(nouvelAbonnement.getId()))
                .filter(abonnement -> abonnement.getPlan().getTransportType() == nouvelAbonnement.getPlan().getTransportType())
                .forEach(abonnement -> {
                    double valeurRestante = calculerRemboursement(abonnement);
                    double prixNouveauPlan = nouvelAbonnement.getPlan().getPrix();

                    // Only refund in downgrade scenario: old remaining > new plan price.
                    // In upgrade, user already paid (newPrice - oldRemaining), so no refund owed.
                    double montantRemboursement = valeurRestante > prixNouveauPlan
                            ? valeurRestante - prixNouveauPlan
                            : 0.0;

                    abonnement.setStatut(StatutAbonnement.ANNULE);
                    abonnement.setDateAnnulation(LocalDateTime.now());
                    abonnementRepository.save(abonnement);
                    log.info("[PLAN-CHANGE] Ancien abonnement {} annulé suite au changement vers l'abonnement {}",
                            abonnement.getId(), nouvelAbonnement.getId());

                    if (montantRemboursement > 0 && abonnement.getPaiementId() != null) {
                        com.serviceabonnement.dto.external.RefundRequestDTO refundRequest = com.serviceabonnement.dto.external.RefundRequestDTO.builder()
                                .transactionId(abonnement.getPaiementId())
                                .montantRemboursement(montantRemboursement)
                                .motif("Remboursement partiel suite à downgrade de plan (différence = oldRemaining - newPrice)")
                                .build();
                        try {
                            paiementClient.rembourser(refundRequest);
                            log.info("[PLAN-CHANGE] Remboursement partiel de {} DH initié pour l'ancien abonnement {}",
                                    montantRemboursement, abonnement.getId());
                        } catch (Exception e) {
                            // Non-fatal: new plan is already activated. Log for manual retry.
                            log.error("[PLAN-CHANGE] Échec du remboursement partiel pour l'abonnement {} (sera retenté manuellement): {}",
                                    abonnement.getId(), e.getMessage());
                        }
                    } else {
                        log.info("[PLAN-CHANGE] Upgrade détecté — aucun remboursement dû pour l'ancien abonnement {}",
                                abonnement.getId());
                    }
                });
    }

    // ─── JWT & Security Helpers ────────────────────────────────────────────────

    private List<String> extractUserRoles() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList());
        }
        return java.util.List.of();
    }

    private boolean isEligibleForPlan(List<String> userRoles, String planCategory) {
        return userRoles.stream()
            .anyMatch(role -> role.equalsIgnoreCase(planCategory));
    }

    // ─── Fallbacks ────────────────────────────────────────────────────────────

    public Abonnement souscrireFallback(Long userId, Long planId, String email, Throwable t) {
        log.error("Fallback critique souscrire pour l'utilisateur {} - Plan {}. Raison: {}", userId, planId, t.getMessage());
        // En cas de fallback ultime, on tente de renvoyer l'abonnement s'il a été créé (idempotence)
        return abonnementRepository.findByUserIdAndPlanIdPlanAndStatut(userId, planId, StatutAbonnement.EN_ATTENTE_PAIEMENT)
                .orElse(null);
    }

    public void annulationFallback(Long abonnementId, Throwable t) {
        log.error("Fallback annulation pour l'abonnement {}. Raison: {}", abonnementId, t.getMessage());
        // On ne jette pas d'exception, on logue et on laisse le système dans l'état actuel 
        // (l'utilisateur pourra réessayer ou le scheduler traitera les états incohérents).
    }

    public Abonnement renouvellementFallback(Long abonnementId, Throwable t) {
        if (t instanceof BaseException baseException) {
            throw baseException;
        }
        log.error("Fallback renouvellement pour l'abonnement {}. Raison: {}", abonnementId, t.getMessage());
        return abonnementRepository.findById(abonnementId).orElse(null);
    }

    private UserDTO fetchUserWithFallback(Long userId, String localEmail) {
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("G3 indisponible lors du traitement pour l'utilisateur {}, utilisation de l'email local : {}", userId, localEmail);
            return UserDTO.builder()
                    .id(userId)
                    .email(localEmail != null ? localEmail : "")
                    .build();
        }
    }

    private String getRandomPaymentMethod() {
        return new java.util.Random().nextBoolean() ? "CARD" : "MOBILE_MONEY";
    }
}
