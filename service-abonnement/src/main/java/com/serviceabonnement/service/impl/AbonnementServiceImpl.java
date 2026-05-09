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
import com.serviceabonnement.exception.PaiementServiceException;
import com.serviceabonnement.exception.PlanNotFoundException;
import com.serviceabonnement.exception.RegleMetierException;
import com.serviceabonnement.exception.UtilisateurNotFoundException;
import com.serviceabonnement.producer.SubscriptionEventPublisher;
import com.serviceabonnement.repository.AbonnementRepository;
import com.serviceabonnement.repository.AnalytiqueTraceRepository;
import com.serviceabonnement.repository.PlanAbonnementRepository;
import com.serviceabonnement.service.AbonnementService;
import com.serviceabonnement.entity.AnalytiqueTrace;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @Override
    @Transactional
    @CircuitBreaker(name = "externalService", fallbackMethod = "souscrireFallback")
    @Retry(name = "externalService")
    @Bulkhead(name = "externalService")
    public Abonnement souscrire(Long userId, Long planId) {
        log.info("Tentative de souscription pour l'utilisateur {} au plan {}", userId, planId);

        // 1. Vérification Utilisateur
        UserDTO user = userClient.getUserById(userId);
        if (user == null || !user.isActive()) {
            throw new UtilisateurNotFoundException(userId);
        }

        // 2. Récupération du Plan
        PlanAbonnement plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        // 3. Création de l'abonnement (Statut initial : EN_ATTENTE de paiement ou SUSPENDU avant activation)
        // Note: Dans ce flux, on va considérer qu'on crée l'abonnement en attente
        Abonnement abonnement = Abonnement.builder()
                .userId(userId)
                .plan(plan)
                .prixPaye(plan.getPrix()) // Snapshot du prix
                .statut(StatutAbonnement.EN_ATTENTE_PAIEMENT) // En attente de paiement
                .renouvellementAuto(true)
                .build();

        abonnement = abonnementRepository.save(abonnement);

        // Envoi au système d'analyse (REST)
        sendToAnalyse(abonnement, "SOUSCRIPTION_INITIALE", plan.getPrix());

        // 4. Initiation Paiement (G6)
        try {
            PaymentRequestDTO paymentRequest = PaymentRequestDTO.builder()
                    .userId(userId)
                    .sourceType("SUBSCRIPTION")
                    .sourceId(abonnement.getId())
                    .amount(plan.getPrix())
                    .paymentMethod(getRandomPaymentMethod())
                    .email(user.getEmail())
                    .description("Souscription plan " + plan.getNomPlan())
                    .build();

            PaymentResponseDTO paymentResponse = paiementClient.initierPaiement(paymentRequest);
            abonnement.setPaiementId(paymentResponse.getTransactionId());
            abonnementRepository.save(abonnement);

        } catch (Exception e) {
            log.error("Échec de l'initiation du paiement pour l'abonnement {}", abonnement.getId());
            eventPublisher.publishEchecSouscription(user, plan.getNomPlan(), "Erreur service paiement");
            throw new PaiementServiceException("Impossible d'initier le paiement", e);
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

    private final com.serviceabonnement.repository.DesactivationRepository desactivationRepository;
    private final com.serviceabonnement.repository.RenouvellementRepository renouvellementRepository;

    @Override
    @Transactional
    public void forcerRenouvellement(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        // Même logique que renouveler mais marqué comme MANUEL
        UserDTO user = userClient.getUserById(abonnement.getUserId());
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
        
        com.serviceabonnement.entity.Renouvellement renouvellement = com.serviceabonnement.entity.Renouvellement.builder()
                .abonnement(abonnement)
                .paiementId(response.getTransactionId())
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.MANUEL)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
        renouvellementRepository.save(renouvellement);

        eventPublisher.publishRenouvellementEffectue(
                userClient.getUserById(abonnement.getUserId()),
                abonnement,
                "MANUEL"
        );
    }

    @Override
    @Transactional
    public void desactiver(Long abonnementId, int jours) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        // Vérification des règles de désactivation du plan
        if (jours > plan.getMaxPeriodeDesactivation()) {
            throw new RegleMetierException(
                "Durée de désactivation " + jours + " jours dépasse le maximum autorisé (" + plan.getMaxPeriodeDesactivation() + " jours)"
            );
        }

        // Logique de pause
        abonnement.setStatut(StatutAbonnement.DESACTIVE);
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

        eventPublisher.publishDesactivationEffectuee(
                userClient.getUserById(abonnement.getUserId()),
                plan.getNomPlan(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(jours),
                1, // Valeur exemple pour nbUsees
                plan.getMaxPeriodeDesactivation()
        );
    }

    @Override
    @Transactional
    public void suspendre(Long abonnementId, String motif) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        if (abonnement.getStatut() == StatutAbonnement.ANNULE ||
                abonnement.getStatut() == StatutAbonnement.SUSPENDU) {
            throw new AbonnementStatutInvalideException(
                "Impossible de suspendre un abonnement au statut : " + abonnement.getStatut());
        }
        abonnement.setStatut(StatutAbonnement.SUSPENDU);
        abonnementRepository.save(abonnement);

        eventPublisher.publishSuspensionEffectuee(
                userClient.getUserById(abonnement.getUserId()),
                abonnement.getPlan().getNomPlan(),
                "ROLE_ADMIN_G2", // adminId par défaut
                motif,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "externalService", fallbackMethod = "annulationFallback")
    @Retry(name = "externalService")
    public void demanderAnnulation(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        if (abonnement.getStatut() == StatutAbonnement.ANNULE ||
                abonnement.getStatut() == StatutAbonnement.ANNULATION_EN_COURS) {
            throw new AbonnementStatutInvalideException(
                "L'abonnement est déjà annulé ou en cours d'annulation : " + abonnement.getStatut());
        }
        
        // Calcul des jours restants avant la fin de l'abonnement
        long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), abonnement.getDateFin());
        Double montantRemboursement = 0.0;

        if (joursRestants >= 3) {
            montantRemboursement = calculerRemboursement(abonnement);
        } else {
            log.info("Annulation de l'abonnement {} sans remboursement car il reste moins de 3 jours ({} jours restants)", abonnementId, joursRestants);
        }
        
        abonnement.setStatut(StatutAbonnement.ANNULATION_EN_COURS);
        abonnement.setDateDemandeAnnulation(LocalDateTime.now());
        abonnementRepository.save(abonnement);

        // Appel G6 pour le remboursement uniquement si le montant est supérieur à 0
        if (montantRemboursement > 0) {
            com.serviceabonnement.dto.external.RefundRequestDTO refundRequest = com.serviceabonnement.dto.external.RefundRequestDTO.builder()
                    .transactionId(abonnement.getPaiementId())
                    .montantRemboursement(montantRemboursement)
                    .motif("Annulation utilisateur - Franchise de 3 jours respectée")
                    .build();
            paiementClient.rembourser(refundRequest);
        }

        eventPublisher.publishAnnulationEffectuee(
                userClient.getUserById(abonnement.getUserId()),
                abonnement,
                montantRemboursement,
                abonnement.getPaiementId()
        );
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
    public Abonnement renouvelerManuel(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        log.info("Renouvellement manuel demandé pour l'abonnement {}", abonnementId);

        UserDTO user = userClient.getUserById(abonnement.getUserId());
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
        
        Renouvellement renouvellement = Renouvellement.builder()
                .abonnement(abonnement)
                .paiementId(response.getTransactionId())
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.MANUEL)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
        renouvellementRepository.save(renouvellement);

        eventPublisher.publishRenouvellementEffectue(
                userClient.getUserById(abonnement.getUserId()),
                abonnement,
                "MANUEL"
        );

        return abonnement;
    }

    @Override
    @Transactional
    public Abonnement renouveler(Long abonnementId) {
        Abonnement abonnement = getAbonnementById(abonnementId);
        PlanAbonnement plan = abonnement.getPlan();

        log.info("Renouvellement automatique pour l'abonnement {}", abonnementId);

        UserDTO user = userClient.getUserById(abonnement.getUserId());
        // Initiation Paiement
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
        
        Renouvellement renouvellement = Renouvellement.builder()
                .abonnement(abonnement)
                .paiementId(response.getTransactionId())
                .dateRenouvellement(LocalDateTime.now())
                .prixApplique(plan.getPrix())
                .typeRenouvellement(com.serviceabonnement.enums.TypeRenouvellement.AUTOMATIQUE)
                .statut(com.serviceabonnement.enums.StatutRenouvellement.EN_ATTENTE)
                .build();
        renouvellementRepository.save(renouvellement);
        
        eventPublisher.publishRenouvellementEffectue(
                userClient.getUserById(abonnement.getUserId()),
                abonnement,
                "AUTOMATIQUE"
        );

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
                userClient.getUserById(abonnement.getUserId()),
                abonnement,
                0.0, // Pas de remboursement automatique sur annulation admin forcée ici
                null
        );
    }

    @Override
    @Transactional
    public void confirmerPaiement(com.serviceabonnement.dto.external.PaymentCallbackDTO callback) {
        // On cherche l'abonnement par le token de transaction reçu
        Abonnement abonnement = abonnementRepository.findByPaiementId(callback.getTransactionToken())
                .orElseThrow(() -> new AbonnementNotFoundException(
                        "Aucun abonnement associé au token de transaction " + callback.getTransactionToken()));

        if ("SUCCESS".equals(callback.getStatus())) {
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
            
            log.info("Abonnement {} activé avec succès (Token: {})", abonnement.getId(), callback.getTransactionToken());
            sendToAnalyse(abonnement, "SOUSCRIPTION_CONFIRMEE", abonnement.getPrixPaye());
            eventPublisher.publishConfirmationSouscription(userClient.getUserById(abonnement.getUserId()), abonnement);
        } else {
            // Si le statut n'est pas SUCCESS, on considère que c'est un échec
            abonnement.setStatut(StatutAbonnement.ECHEC_PAIEMENT);
            log.warn("Le paiement a échoué pour l'abonnement {} : {}", abonnement.getId(), callback.getMessage());
        }
        
        abonnementRepository.save(abonnement);
    }

    @Override
    @Transactional
    public void confirmerRemboursement(com.serviceabonnement.dto.external.RefundCallbackDTO callback) {
        Abonnement abonnement = abonnementRepository.findByPaiementId(callback.getTransactionId())
                .orElseGet(() -> abonnementRepository.findByRemboursementId(callback.getTransactionId())
                .orElseThrow(() -> new AbonnementNotFoundException(
                        "Aucun abonnement associé à la transaction " + callback.getTransactionId())));

        switch (callback.getStatut()) {
            case "REMBOURSE" -> {
                abonnement.setStatut(StatutAbonnement.ANNULE);
                abonnement.setDateAnnulation(LocalDateTime.now());
                log.info("Abonnement {} annulé avec succès (Remboursement effectué)", abonnement.getId());
                sendToAnalyse(abonnement, "ANNULATION_CONFIRMEE", 0.0);
                eventPublisher.publishAnnulationEffectuee(userClient.getUserById(abonnement.getUserId()), abonnement, callback.getMontantRembourse(), callback.getTransactionId());
            }
            case "ECHEC_REMBOURSEMENT" -> {
                abonnement.setStatut(StatutAbonnement.ECHEC_REMBOURSEMENT);
                log.error("Échec définitif du remboursement pour l'abonnement {} : {}", abonnement.getId(), callback.getMotif());
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
            UserDTO user = userClient.getUserById(abonnement.getUserId());
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
            log.info("Trace d'analyse collectée pour l'utilisateur {} - action: {}", trace.getUserId(), formattedAction);

        } catch (Exception e) {
            log.error("Échec de la collecte de la trace d'analyse: {}", e.getMessage());
        }
    }

    private String mapPlanType(com.serviceabonnement.enums.DureeOffre duree, com.serviceabonnement.enums.CategorieAbonnement categorie) {
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

    // ─── Fallbacks ────────────────────────────────────────────────────────────

    public Abonnement souscrireFallback(Long userId, Long planId, Throwable t) {
        log.error("Fallback souscrire pour l'utilisateur {} - Plan {}. Raison: {}", userId, planId, t.getMessage());
        throw new com.serviceabonnement.exception.ExternalServiceException(
            "Le service de souscription est momentanément indisponible. Veuillez réessayer plus tard.");
    }

    public void annulationFallback(Long abonnementId, Throwable t) {
        log.error("Fallback annulation pour l'abonnement {}. Raison: {}", abonnementId, t.getMessage());
        throw new com.serviceabonnement.exception.ExternalServiceException(
            "Le service d'annulation est momentanément indisponible. Votre demande a été enregistrée mais le remboursement sera traité ultérieurement.");
    }

    private String getRandomPaymentMethod() {
        return java.util.Random.class.getName().equals("java.util.Random") ? 
            (new java.util.Random().nextBoolean() ? "CARD" : "MOBILE_MONEY") : "CARD";
    }
}
