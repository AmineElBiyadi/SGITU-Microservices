package com.ensate.billetterie.ticket.service;

import com.ensate.billetterie.event.config.KafkaTopics;
import com.ensate.billetterie.event.interfaces.EventPublisher;
import com.ensate.billetterie.exceptions.TicketNotFoundException;
import com.ensate.billetterie.exceptions.TicketOperationException;
import com.ensate.billetterie.ticket.domain.entity.Ticket;
import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import com.ensate.billetterie.ticket.dto.request.admin.AdminCancelRequest;
import com.ensate.billetterie.ticket.dto.request.admin.FlagResolveRequest;
import com.ensate.billetterie.ticket.dto.request.admin.ForceRefundRequest;
import com.ensate.billetterie.ticket.dto.request.admin.FraudConfirmRequest;
import com.ensate.billetterie.ticket.dto.response.AuditEntryResponse;
import com.ensate.billetterie.ticket.dto.response.DashboardResponse;
import com.ensate.billetterie.ticket.dto.response.TicketResponse;
import com.ensate.billetterie.ticket.mapper.TicketMapper;
import com.ensate.billetterie.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTicketService  {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final EventPublisher<Object> eventPublisher;



    public List<TicketResponse> getAllTickets() {
        return ticketMapper.toResponseList(ticketRepository.findAll());
    }

    public List<TicketResponse> getFlaggedTickets() {
        List<Ticket> flagged = ticketRepository.findByStatusIn(List.of(TicketStatus.FLAGGED));
        return ticketMapper.toResponseList(flagged);
    }


    public TicketResponse getFlaggedTicket(String ticketId) {
        Ticket ticket = findOrThrow(ticketId);
        assertStatus(ticket, TicketStatus.FLAGGED);
        return ticketMapper.toResponse(ticket);
    }


    public TicketResponse resolveFlaggedTicket(String ticketId, FlagResolveRequest request) {
        Ticket ticket = findOrThrow(ticketId);
        assertStatus(ticket, TicketStatus.FLAGGED);


        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setFlaggedAt(null);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Flag resolved on ticket={} by admin={}", ticketId, request.getResolvedBy());

        eventPublisher.publish(KafkaTopics.TICKET_FLAG_REVIEWED, Map.of(
                "ticketId",       saved.getId(),
                "resolvedBy",     request.getResolvedBy(),
                "resolutionNote", request.getResolutionNote(),
                "outcome",        "RESOLVED"
        ));

        return ticketMapper.toResponse(saved);
    }


    public TicketResponse confirmFraud(String ticketId, FraudConfirmRequest request) {
        Ticket ticket = findOrThrow(ticketId);
        assertStatus(ticket, TicketStatus.FLAGGED);

        ticket.cancel();
        ticket.setCancelledAt(Instant.now());

        Ticket saved = ticketRepository.save(ticket);
        log.warn("Fraud confirmed on ticket={} by admin={}", ticketId, request.getConfirmedBy());

        eventPublisher.publish(KafkaTopics.TICKET_FLAG_REVIEWED, Map.of(
                "ticketId",         saved.getId(),
                "confirmedBy",      request.getConfirmedBy(),
                "fraudReason",      request.getFraudReason(),
                "blacklistHolder",  request.isBlacklistHolder(),
                "outcome",          "FRAUD_CONFIRMED"
        ));
        eventPublisher.publish(KafkaTopics.TICKET_CANCELLED, saved);

        return ticketMapper.toResponse(saved);
    }




    public TicketResponse adminCancelTicket(String ticketId, AdminCancelRequest request) {
        Ticket ticket = findOrThrow(ticketId);


        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new TicketOperationException("Cannot cancel a ticket that has already been refunded");
        }

        ticket.cancel();
        ticket.setCancelledAt(Instant.now());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Admin cancel ticket={} by={} reason={}", ticketId,
                request.getCancelledBy(), request.getReason());

        eventPublisher.publish(KafkaTopics.TICKET_CANCELLED, Map.of(
                "ticket",      saved,
                "cancelledBy", request.getCancelledBy(),
                "reason",      request.getReason(),
                "adminAction", true
        ));

        return ticketMapper.toResponse(saved);
    }


    public TicketResponse forceRefund(String ticketId, ForceRefundRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new TicketOperationException("Ticket has already been refunded");
        }

        ticket.setStatus(TicketStatus.REFUND_PENDING);
        //Communicate with payment service
        ticket.setStatus(TicketStatus.REFUNDED);
        ticket.setRefundedAt(Instant.now());

        Ticket saved = ticketRepository.save(ticket);
        log.warn("Force-refund applied to ticket={} by admin={}", ticketId, request.getRefundedBy());

        eventPublisher.publish(KafkaTopics.TICKET_REFUNDED, Map.of(
                "ticket",      saved,
                "refundedBy",  request.getRefundedBy(),
                "reason",      request.getReason(),
                "forced",      true
        ));

        return ticketMapper.toResponse(saved);
    }

    // ─── Dashboard ────────────────────────────────────────────────────


    public DashboardResponse getDashboard() {
        List<Ticket> all = ticketRepository.findAll();

        long issued      = countByStatus(all, TicketStatus.ISSUED);
        long redeemed    = countByStatus(all, TicketStatus.REDEEMED);
        long cancelled   = countByStatus(all, TicketStatus.CANCELLED);
        long refunded    = countByStatus(all, TicketStatus.REFUNDED);
        long transferred = countByStatus(all, TicketStatus.TRANSFERRED);
        long expired     = countByStatus(all, TicketStatus.EXPIRED);
        long flagged     = countByStatus(all, TicketStatus.FLAGGED);

        double totalRevenue    = all.stream().mapToDouble(Ticket::getPrice).sum();
        double refundedRevenue = all.stream()
                .filter(t -> t.getStatus() == TicketStatus.REFUNDED)
                .mapToDouble(Ticket::getPrice).sum();

        long transfersInitiated  = all.stream()
                .mapToLong(t -> t.getTransferHistory() == null ? 0 : t.getTransferHistory().size())
                .sum();

        return DashboardResponse.builder()
                .totalTickets(all.size())
                .issuedCount(issued)
                .redeemedCount(redeemed)
                .cancelledCount(cancelled)
                .refundedCount(refunded)
                .transferredCount(transferred)
                .expiredCount(expired)
                .flaggedCount(flagged)
                .totalRevenue(totalRevenue)
                .refundedRevenue(refundedRevenue)
                .netRevenue(totalRevenue - refundedRevenue)
                .transfersInitiated(transfersInitiated)
                .generatedAt(Instant.now())
                .build();
    }

    // ─── Audit trail ──────────────────────────────────────────────────

    /**
     * Reconstructs the audit trail from the ticket's transfer history plus
     * the key lifecycle timestamps stamped on the entity itself.
     *
     * In a production system this would typically be backed by a dedicated
     * AuditLog collection; this implementation derives entries from the data
     * already present on the Ticket document.
     */

    public List<AuditEntryResponse> getAuditTrail(String ticketId) {
        Ticket ticket = findOrThrow(ticketId);
        List<AuditEntryResponse> trail = new java.util.ArrayList<>();
        int seq = 1;

        // 1. Issuance
        trail.add(AuditEntryResponse.builder()
                .sequence(seq++)
                .fromStatus(null)
                .toStatus(TicketStatus.ISSUED)
                .action("Ticket issued")
                .occurredAt(ticket.getIssuedAt())
                .build());

        // 2. Transfer steps (in order)
        if (ticket.getTransferHistory() != null) {
            for (var record : ticket.getTransferHistory()) {
                trail.add(AuditEntryResponse.builder()
                        .sequence(seq++)
                        .fromStatus(TicketStatus.ISSUED)
                        .toStatus(TicketStatus.TRANSFERRED)
                        .action("Ticket transferred from " + record.getFromHolder()
                                + " to " + record.getToHolder())
                        .performedBy(record.getFromHolder())
                        .occurredAt(record.getTransferredAt())
                        .reason(record.getReason())
                        .build());
            }
        }

        // 3. Redemption
        if (ticket.getRedeemedAt() != null) {
            trail.add(AuditEntryResponse.builder()
                    .sequence(seq++)
                    .toStatus(TicketStatus.REDEEMED)
                    .action("Ticket validated and redeemed")
                    .occurredAt(ticket.getRedeemedAt())
                    .build());
        }

        // 4. Flag
        if (ticket.getFlaggedAt() != null) {
            trail.add(AuditEntryResponse.builder()
                    .sequence(seq++)
                    .toStatus(TicketStatus.FLAGGED)
                    .action("Ticket flagged for suspicious activity")
                    .occurredAt(ticket.getFlaggedAt())
                    .build());
        }

        // 5. Cancellation
        if (ticket.getCancelledAt() != null) {
            trail.add(AuditEntryResponse.builder()
                    .sequence(seq++)
                    .toStatus(TicketStatus.CANCELLED)
                    .action("Ticket cancelled")
                    .occurredAt(ticket.getCancelledAt())
                    .build());
        }

        // 6. Refund
        if (ticket.getRefundedAt() != null) {
            trail.add(AuditEntryResponse.builder()
                    .sequence(seq++)
                    .toStatus(TicketStatus.REFUNDED)
                    .action("Ticket refunded")
                    .occurredAt(ticket.getRefundedAt())
                    .build());
        }

        // Sort chronologically in case timestamps were written out of order
        trail.sort(java.util.Comparator.comparing(
                e -> e.getOccurredAt() != null ? e.getOccurredAt() : Instant.EPOCH));

        // Re-sequence after sort
        for (int i = 0; i < trail.size(); i++) trail.get(i).setSequence(i + 1);

        return trail;
    }



    private Ticket findOrThrow(String id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + id));
    }

    private void assertStatus(Ticket ticket, TicketStatus expected) {
        if (ticket.getStatus() != expected) {
            throw new TicketOperationException(
                    "Expected ticket status " + expected + " but found " + ticket.getStatus());
        }
    }

    private long countByStatus(List<Ticket> tickets, TicketStatus status) {
        return tickets.stream().filter(t -> t.getStatus() == status).count();
    }
}
