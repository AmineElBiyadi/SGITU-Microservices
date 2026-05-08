package com.ensate.billetterie.ticket.controller;

import com.ensate.billetterie.ticket.dto.request.admin.AdminCancelRequest;
import com.ensate.billetterie.ticket.dto.request.admin.FlagResolveRequest;
import com.ensate.billetterie.ticket.dto.request.admin.ForceRefundRequest;
import com.ensate.billetterie.ticket.dto.request.admin.FraudConfirmRequest;
import com.ensate.billetterie.ticket.dto.response.AuditEntryResponse;
import com.ensate.billetterie.ticket.dto.response.DashboardResponse;
import com.ensate.billetterie.ticket.dto.response.TicketResponse;
import com.ensate.billetterie.ticket.service.AdminTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin – Ticket Management", description = "Administrative operations on tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final AdminTicketService adminTicketService;


    @GetMapping("/tickets")
    @Operation(summary = "Get all tickets")
    public ResponseEntity<List<TicketResponse>> getTickets() {
        return ResponseEntity.ok(adminTicketService.getAllTickets());
    }

    // ─── Flagged tickets ──────────────────────────────────────────────────

    @GetMapping("/tickets/flagged")
    @Operation(summary = "List all flagged tickets",
            description = "Returns every ticket currently in FLAGGED status for admin review.")
    public ResponseEntity<List<TicketResponse>> getFlaggedTickets() {
        return ResponseEntity.ok(adminTicketService.getFlaggedTickets());
    }

    @GetMapping("/tickets/{ticketId}/flagged")
    @Operation(summary = "Get flagged ticket details",
            description = "Returns the full detail of a single FLAGGED ticket, including transfer history and metadata.")
    public ResponseEntity<TicketResponse> getFlaggedTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(adminTicketService.getFlaggedTicket(ticketId));
    }

    @PutMapping("/tickets/{ticketId}/flag/resolve")
    @Operation(summary = "Resolve a flagged ticket",
            description = "Clears the FLAGGED status and restores the ticket to ISSUED or TRANSFERRED after admin review finds no issue.")
    public ResponseEntity<TicketResponse> resolveFlaggedTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody FlagResolveRequest request) {
        return ResponseEntity.ok(adminTicketService.resolveFlaggedTicket(ticketId, request));
    }

    @PutMapping("/tickets/{ticketId}/flag/confirmfraud")
    @Operation(summary = "Confirm fraud on a flagged ticket",
            description = "Marks the ticket as fraudulent, cancels it, and triggers downstream fraud-reporting events.")
    public ResponseEntity<TicketResponse> confirmFraud(
            @PathVariable String ticketId,
            @Valid @RequestBody FraudConfirmRequest request) {
        return ResponseEntity.ok(adminTicketService.confirmFraud(ticketId, request));
    }

    // ─── Manual admin actions ─────────────────────────────────────────────

    @DeleteMapping("/tickets/{ticketId}")
    @Operation(summary = "Manually cancel a ticket",
            description = "Hard-cancels any ticket regardless of its current status. Reserved for admin use only.")
    public ResponseEntity<TicketResponse> adminCancelTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody AdminCancelRequest request) {
        return ResponseEntity.ok(adminTicketService.adminCancelTicket(ticketId, request));
    }

    @PostMapping("/tickets/{ticketId}/forcerefund")
    @Operation(summary = "Force a refund",
            description = "Bypasses normal refund prerequisites (e.g. ticket not yet cancelled) and immediately issues a refund.")
    public ResponseEntity<TicketResponse> forceRefund(
            @PathVariable String ticketId,
            @Valid @RequestBody ForceRefundRequest request) {
        return ResponseEntity.ok(adminTicketService.forceRefund(ticketId, request));
    }

    // ─── Dashboard & audit ────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard statistics",
            description = "Returns aggregated ticket statistics: counts by status, revenue totals, recent activity, and fraud metrics.")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminTicketService.getDashboard());
    }

    @GetMapping("/tickets/{ticketId}/audit")
    @Operation(summary = "Get ticket audit trail",
            description = "Returns the complete ordered audit log for a ticket — every status transition, actor, timestamp, and reason.")
    public ResponseEntity<List<AuditEntryResponse>> getAuditTrail(@PathVariable String ticketId) {
        return ResponseEntity.ok(adminTicketService.getAuditTrail(ticketId));
    }
}
