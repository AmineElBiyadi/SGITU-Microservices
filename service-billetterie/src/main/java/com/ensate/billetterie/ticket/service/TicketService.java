package com.ensate.billetterie.ticket.service;

import com.ensate.billetterie.event.config.KafkaTopics;
import com.ensate.billetterie.event.interfaces.EventPublisher;
import com.ensate.billetterie.exceptions.TicketNotFoundException;
import com.ensate.billetterie.exceptions.TicketOperationException;
import com.ensate.billetterie.identity.dto.IssueTokenRequest;
import com.ensate.billetterie.identity.dto.IssueTokenResponse;
import com.ensate.billetterie.identity.service.IdentityService;
import com.ensate.billetterie.ticket.client.CoordinationClient;
import com.ensate.billetterie.ticket.client.PaymentServiceClient;
import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.ticket.domain.entity.TransferRecord;
import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import com.ensate.billetterie.ticket.dto.request.*;
import com.ensate.billetterie.ticket.dto.response.PaymentResponse;
import com.ensate.billetterie.ticket.dto.response.TicketResponse;
import com.ensate.billetterie.ticket.dto.result.ValidationResult;
import com.ensate.billetterie.ticket.mapper.TicketMapper;
import com.ensate.billetterie.ticket.repository.TicketRepository;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.pipeline.ValidationPipeline;
import com.ensate.billetterie.validation.steps.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketRepository ticketRepository;
    private final CoordinationClient coordinationClient;
    private final IdentityService identityService;
    private final Executor validationExecutor;
    private final EventPublisher<Object> eventPublisher;
    private final PaymentServiceClient paymentServiceClient;



    public List<TicketResponse> getAllTickets() {
        return ticketMapper.toResponseList(ticketRepository.findAll());
    }


    public TicketResponse getTicketById(String ticketId) {
        return ticketMapper.toResponse(findOrThrow(ticketId));
    }


    public List<TicketResponse> getTicketsByUser(String userId) {
        return ticketMapper.toResponseList(ticketRepository.findByHolderId(userId));
    }




    public TicketResponse createTicket(CreateTicketRequest request) {

        Ticket ticket = ticketMapper.toEntity(request);

        // Issue Token
        IssueTokenRequest tokenReq = IssueTokenRequest.builder()
                .holderId(request.getHolderId())
                .eventId(request.getTripId())
                .methodType(request.getIdentityMethod())
                //.rawPayload(request.getRawPayload())
                .build();

        IssueTokenResponse tokenResp = identityService.issue(tokenReq);
        ticket.setTokenValue(tokenResp.getTokenValue());


        ticket.setStatus(TicketStatus.CREATED);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket created: id={}, holder={}", saved.getId(), saved.getHolderId());



        return ticketMapper.toResponse(saved);
    }




    public TicketResponse validateTicket(String ticketId, ValidateTicketRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.ISSUED, TicketStatus.TRANSFERRED);

        if (ticket.isExpired()) {
            ticket.markExpired();
            ticketRepository.save(ticket);
            throw new TicketOperationException("Ticket " + ticketId + " has expired");
        }


        CompletableFuture<ValidationResult> result = validateTicket(request);

        result.thenAccept(validationResult -> {
            boolean valid = validationResult.isValid();
            if (!valid) {
                eventPublisher.publish(KafkaTopics.TICKET_FLAGGED, ticket);
                throw new TicketOperationException("Token verification failed for ticket " + ticketId);
            }else{
                ticket.markRedeemed();
                ticket.setRedeemedAt(Instant.now());

            }
        });


        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publish(KafkaTopics.TICKET_VALIDATED, saved);
        return ticketMapper.toResponse(saved);
    }


    public TicketResponse payTicket(String ticketId, PaymentRequest paymentRequest) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.CREATED);

        if (ticket.isExpired()) {
            ticket.markExpired();
            ticketRepository.save(ticket);
            throw new TicketOperationException("Ticket " + ticketId + " has expired");
        }


        PaymentResponse paymentResponse = paymentServiceClient.pay(paymentRequest);

        if(paymentResponse.getPaymentStatus().equals("FAILED") ||
                paymentResponse.getInvoiceId() == null ||
                paymentResponse.getInvoiceNumber() == null) {

            eventPublisher.publish(KafkaTopics.TICKET_PAYMENT_FAILED, ticket);
            return ticketMapper.toResponse(ticket);
        }



        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setIssuedAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publish(KafkaTopics.TICKET_PAYMENT_SUCCESS, saved);
        return ticketMapper.toResponse(saved);
    }




    public TicketResponse cancelTicket(String ticketId) {
        Ticket ticket = findOrThrow(ticketId);

        assertNotStatus(ticket, TicketStatus.CANCELLED, TicketStatus.REDEEMED,
                TicketStatus.REFUNDED, TicketStatus.EXPIRED);

        ticket.cancel();
        ticket.setCancelledAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publish(KafkaTopics.TICKET_CANCELLED, saved);
        return ticketMapper.toResponse(saved);
    }




    public TicketResponse refundTicket(String ticketId) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.CANCELLED);


        ticket.setStatus(TicketStatus.REFUND_PENDING);

        //Communicate with payment service here to initiate refund
        //Modify ticket state if refund effected

        ticket.setRefundedAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publish(KafkaTopics.TICKET_REFUND_REQUESTED, saved);
        return ticketMapper.toResponse(saved);
    }




    public TicketResponse transferTicket(String ticketId, TicketTransferRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.ISSUED, TicketStatus.TRANSFERRED);

        if (ticket.isExpired()) {
            throw new TicketOperationException("Cannot transfer an expired ticket");
        }

        // transferTo() appends a TransferRecord and sets status = TRANSFERRED
        ticket.transferTo(request.getNewHolderId(), request.getReason());
        ticket.setTransferredAt(Instant.now());
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publish(KafkaTopics.TICKET_TRANSFER_INITIATED, saved);
        return ticketMapper.toResponse(saved);
    }




    public TicketResponse acceptTransfer(String ticketId, TicketAcceptRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.TRANSFERRED);
        assertCurrentHolder(ticket, request.getAcceptingUserId());

        // Status stays TRANSFERRED; we just confirm by persisting + publishing
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publish(KafkaTopics.TICKET_TRANSFER_COMPLETED, saved);
        return ticketMapper.toResponse(saved);
    }




    public TicketResponse rejectTransfer(String ticketId, TicketAcceptRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.TRANSFERRED);
        assertCurrentHolder(ticket, request.getAcceptingUserId());

        // Roll back: pop last TransferRecord and restore previous holder
        List<TransferRecord> history = ticket.getTransferHistory();
        if (history == null || history.isEmpty()) {
            throw new TicketOperationException("No transfer record found to reject");
        }

        return getHistoryResponse(ticket, history);
    }




    public TicketResponse cancelTransfer(String ticketId, TicketCancelRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        assertStatus(ticket, TicketStatus.TRANSFERRED);

        List<TransferRecord> history = ticket.getTransferHistory();
        if (history == null || history.isEmpty()) {
            throw new TicketOperationException("No pending transfer to cancel");
        }


        return getHistoryResponse(ticket, history);
    }

    private TicketResponse getHistoryResponse(Ticket ticket, List<TransferRecord> history) {
        TransferRecord last = history.get(history.size() - 1);
        ticket.setHolderId(last.getFromHolder());
        ticket.setStatus(TicketStatus.ISSUED);
        history.remove(history.size() - 1);

        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publish(KafkaTopics.TICKET_TRANSFER_CANCELLED, saved);
        return ticketMapper.toResponse(saved);
    }



    private Ticket findOrThrow(String id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + id));
    }

    private void assertStatus(Ticket ticket, TicketStatus... allowed) {
        for (TicketStatus s : allowed) {
            if (ticket.getStatus() == s) return;
        }
        throw new TicketOperationException(
                "Operation not allowed for ticket in status: " + ticket.getStatus());
    }

    private void assertNotStatus(Ticket ticket, TicketStatus... forbidden) {
        for (TicketStatus s : forbidden) {
            if (ticket.getStatus() == s) {
                throw new TicketOperationException(
                        "Operation not allowed for ticket in status: " + ticket.getStatus());
            }
        }
    }

    private void assertCurrentHolder(Ticket ticket, String userId) {
        if (!ticket.getHolderId().equals(userId)) {
            throw new TicketOperationException(
                    "User " + userId + " is not the current holder of ticket " + ticket.getId());
        }
    }



    private CompletableFuture<ValidationResult> validateTicket(ValidateTicketRequest request) {
        
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
