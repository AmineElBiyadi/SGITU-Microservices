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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin – Ticket Management", description = "Administrative operations on tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminTicketController {

    private final AdminTicketService adminTicketService;


    @GetMapping("/tickets")
    @Operation(summary = "Get all tickets",
            description = "Scope: admin:ticket:read. Returns every ticket in the billetterie service for administrative review and back-office operations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tickets returned successfully. The list can be empty."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<List<TicketResponse>> getTickets() {
        return ResponseEntity.ok(adminTicketService.getAllTickets());
    }

    // ─── Flagged tickets ──────────────────────────────────────────────────

    @GetMapping("/tickets/flagged")
    @Operation(summary = "List all flagged tickets",
            description = "Scope: admin:ticket:flag:read. Returns every ticket currently in FLAGGED status for admin review.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flagged tickets returned successfully. The list can be empty."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<List<TicketResponse>> getFlaggedTickets() {
        return ResponseEntity.ok(adminTicketService.getFlaggedTickets());
    }

    @GetMapping("/tickets/{ticketId}/flagged")
    @Operation(summary = "Get flagged ticket details",
            description = "Scope: admin:ticket:flag:read. Returns the full detail of a single FLAGGED ticket, including transfer history and metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flagged ticket returned successfully."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not currently FLAGGED."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> getFlaggedTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(adminTicketService.getFlaggedTicket(ticketId));
    }

    @PutMapping("/tickets/{ticketId}/flag/resolve")
    @Operation(summary = "Resolve a flagged ticket",
            description = "Scope: admin:ticket:flag:resolve. Clears the FLAGGED status, restores the ticket to ISSUED, and publishes ticket.flag.reviewed with outcome RESOLVED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flag resolved successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not currently FLAGGED."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> resolveFlaggedTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody FlagResolveRequest request) {
        return ResponseEntity.ok(adminTicketService.resolveFlaggedTicket(ticketId, request));
    }

    @PutMapping("/tickets/{ticketId}/flag/confirmfraud")
    @Operation(summary = "Confirm fraud on a flagged ticket",
            description = "Scope: admin:ticket:flag:confirm-fraud. Confirms fraud for a FLAGGED ticket, cancels it, and publishes ticket.flag.reviewed plus ticket.cancelled.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fraud confirmed and ticket cancelled successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not currently FLAGGED."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> confirmFraud(
            @PathVariable String ticketId,
            @Valid @RequestBody FraudConfirmRequest request) {
        return ResponseEntity.ok(adminTicketService.confirmFraud(ticketId, request));
    }

    // ─── Manual admin actions ─────────────────────────────────────────────

    @DeleteMapping("/tickets/{ticketId}")
    @Operation(summary = "Manually cancel a ticket",
            description = "Scope: admin:ticket:cancel. Hard-cancels a ticket for administrative reasons unless it is already REFUNDED. Publishes ticket.cancelled with admin action metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket manually cancelled successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket cannot be cancelled because it has already been refunded."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> adminCancelTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody AdminCancelRequest request) {
        return ResponseEntity.ok(adminTicketService.adminCancelTicket(ticketId, request));
    }

    @PostMapping("/tickets/{ticketId}/forcerefund")
    @Operation(summary = "Force a refund",
            description = "Scope: admin:ticket:refund:force. Bypasses normal refund prerequisites, marks the ticket as REFUNDED, and publishes ticket.refunded with forced refund metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Forced refund completed successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket has already been refunded."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> forceRefund(
            @PathVariable String ticketId,
            @Valid @RequestBody ForceRefundRequest request) {
        return ResponseEntity.ok(adminTicketService.forceRefund(ticketId, request));
    }

    // ─── Dashboard & audit ────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard statistics",
            description = "Scope: admin:ticket:dashboard:read. Returns aggregated ticket statistics including counts by status, revenue totals, refund totals, net revenue, transfer count, and generation timestamp.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard statistics returned successfully."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminTicketService.getDashboard());
    }

    @GetMapping("/tickets/{ticketId}/audit")
    @Operation(summary = "Get ticket audit trail",
            description = "Scope: admin:ticket:audit:read. Returns the ordered audit trail reconstructed from ticket lifecycle timestamps and transfer history, including status transitions, actors, timestamps, and reasons when available.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit trail returned successfully. The list can be empty if no lifecycle timestamps are present."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<List<AuditEntryResponse>> getAuditTrail(@PathVariable String ticketId) {
        return ResponseEntity.ok(adminTicketService.getAuditTrail(ticketId));
    }
}
