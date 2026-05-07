package com.ensate.billetterie.ticket.service;

import com.ensate.billetterie.identity.service.IdentityService;
import com.ensate.billetterie.ticket.client.CoordinationClient;
import com.ensate.billetterie.ticket.dto.request.ValidateTicketRequest;
import com.ensate.billetterie.ticket.dto.result.ValidationResult;
import com.ensate.billetterie.ticket.repository.TicketRepository;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.pipeline.ValidationPipeline;
import com.ensate.billetterie.validation.steps.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CoordinationClient coordinationClient;
    private final IdentityService identityService;
    private final Executor validationExecutor;

    public TicketService(
            TicketRepository ticketRepository,
            CoordinationClient coordinationClient,
            IdentityService identityService,
            @Qualifier("validationExecutor") Executor validationExecutor) {
        this.ticketRepository = ticketRepository;
        this.coordinationClient = coordinationClient;
        this.identityService = identityService;
        this.validationExecutor = validationExecutor;
    }

    /**
     * Valide un billet à travers le pipeline de validation.
     * @param request les données de validation (ticketId, tokenValue, etc.)
     * @return un CompletableFuture contenant le résultat de la validation
     */
    public CompletableFuture<ValidationResult> validateTicket(ValidateTicketRequest request) {
        
        // 1. Construire le contexte de validation initial
        ValidationContext context = ValidationContext.builder()
                .ticketId(request.getTicketId())
                .tokenValue(request.getTokenValue())
                .identityPayload(request.getIdentityPayload())
                // Si on passe d'autres infos (holderId, eventId) dans la requête dans le futur, 
                // on pourra les ajouter ici.
                .build();

        // 2. Configurer le pipeline de validation
        // L'ordre est CRITIQUE pour les performances : 
        // Checks rapides en premier (court-circuit), checks coûteux (réseau/crypto) à la fin.
        ValidationPipeline pipeline = new ValidationPipeline()
                // Fast checks (Base de données et en mémoire)
                .addStep(new TicketExistenceStep(ticketRepository, validationExecutor))
                .addStep(new TicketStatusStep(validationExecutor))
                .addStep(new ExpiryCheckStep(validationExecutor))
                .addStep(new HolderMatchStep(validationExecutor))
                // Expensive checks (Réseau HTTP et algorithmes complexes)
                .addStep(new EventActiveStep(coordinationClient, validationExecutor))
                .addStep(new TokenVerificationStep(identityService, validationExecutor));

        // 3. Exécuter le pipeline (gère les CompletableFuture et ValidationException en interne)
        return pipeline.execute(context);
    }
}
