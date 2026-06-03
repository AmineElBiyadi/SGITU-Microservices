package com.ensate.billetterie.ticket.controller;


import com.ensate.billetterie.ticket.dto.request.*;
import com.ensate.billetterie.ticket.dto.response.TicketResponse;
import com.ensate.billetterie.ticket.dto.response.TicketTransferResponse;
import com.ensate.billetterie.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Normal User – Ticket Management", description = "Normal User Operations for managing ticket operations")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;


    @GetMapping("/{ticketId}")
    @Operation(summary = "Get a single ticket by its ID",
            description = "Scope: ticket:read. Returns the full ticket details for the requested identifier. The caller should be the current holder or a trusted service/admin gateway.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket found and returned successfully."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket exists but cannot be processed because of a business rule."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> getTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all tickets associated with a particular user",
            description = "Scope: ticket:read:self. Returns the complete ticket history for a holder, including active, cancelled, transferred, refunded, expired, and flagged tickets when present.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket history returned successfully. The list can be empty."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<List<TicketResponse>> getUserTicketHistory(@PathVariable String userId) {
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }



    @PostMapping
    @Operation(summary = "Create a new paperless ticket",
            description = "Scope: ticket:create. Creates a ticket in CREATED status and issues an identity token through the identity service. Payment is not captured by this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "422", description = "Ticket cannot be created because of a business rule or downstream identity-service error."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest createTicketRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.createTicket(createTicketRequest));
    }



    @PostMapping("/{ticketId}/validate")
    @Operation(summary = "Validate (redeem) a ticket",
            description = "Scope: ticket:validate. Validates token, holder, expiry, ticket status, and event activity before redeeming the ticket. Publishes ticket.validated when successful and ticket.flagged when verification fails.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket validated and redeemed successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is expired, has an invalid status, or failed validation."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> validateTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody ValidateTicketRequest validateTicketRequest) {
        return ResponseEntity.ok(ticketService.validateTicket(ticketId, validateTicketRequest));
    }


    @PostMapping("/{ticketId}/pay")
    @Operation(summary = "Pay for ticket",
            description = "Scope: ticket:pay. Captures payment for a CREATED ticket through the payment service, changes the status to ISSUED, and publishes ticket.payment.success. Publishes ticket.payment.failed when payment fails.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment accepted and ticket issued."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not payable, user is not the current holder, ticket is expired, or payment failed."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> payTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        return ResponseEntity.ok(ticketService.payTicket(ticketId, paymentRequest));
    }

    @PostMapping("/{ticketId}/cancel")
    @Operation(summary = "Cancel a ticket",
            description = "Scope: ticket:cancel. Cancels a ticket unless it is already cancelled, redeemed, refunded, or expired. Publishes ticket.cancelled after persistence.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket cancelled successfully."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket status does not allow cancellation."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.cancelTicket(ticketId));
    }

    @PostMapping("/{ticketId}/refund")
    @Operation(summary = "Request a refund for a cancelled ticket",
            description = "Scope: ticket:refund. Requests a refund through the payment service and marks the ticket as REFUNDED when successful. Publishes ticket.refund.requested after success and ticket.refund.cancelled on payment-service failure.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed and ticket marked as refunded."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket status does not allow refund or refund failed."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> refundTicket(
            @PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.refundTicket(ticketId));
    }



    @PostMapping("/{ticketId}/transfer")
    @Operation(summary = "Initiate a ticket transfer to a new holder",
            description = "Scope: ticket:transfer. Creates a pending ticket for the new holder, marks the original ticket as TRANSFER_PENDING, issues a token for the new holder, and publishes ticket.transfer.initiated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer initiated successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket status, expiry, or business rules do not allow transfer."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketTransferResponse> transferTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketTransferRequest ticketTransferRequest) {
        return ResponseEntity.ok(ticketService.transferTicket(ticketId, ticketTransferRequest));
    }

    @PostMapping("/{ticketId}/transfer/accept")
    @Operation(summary = "Accept a pending ticket transfer",
            description = "Scope: ticket:transfer:accept. Accepts a TRANSFER_PENDING ticket for the new holder, issues it, updates the original ticket as transferred, and publishes ticket.transfer.completed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer accepted successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Pending or original ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not pending transfer or requester is not the accepting holder."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> acceptTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketAcceptRequest ticketAcceptRequest) {
        return ResponseEntity.ok(ticketService.acceptTransfer(ticketId, ticketAcceptRequest));
    }

    @PostMapping("/{ticketId}/transfer/reject")
    @Operation(summary = "Reject a pending ticket transfer",
            description = "Scope: ticket:transfer:reject. Rejects a pending transfer, restores the original ticket to ISSUED, cancels the pending ticket, and publishes ticket.transfer.rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer rejected successfully."),
            @ApiResponse(responseCode = "400", description = "Request body validation failed."),
            @ApiResponse(responseCode = "404", description = "Pending or original ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not pending transfer or requester is not the pending holder."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketTransferResponse> rejectTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketAcceptRequest ticketAcceptRequest) {
        return ResponseEntity.ok(ticketService.rejectTransfer(ticketId, ticketAcceptRequest));
    }


    @PostMapping("/{ticketId}/transfer/cancel")
    @Operation(summary = "Cancel a pending ticket transfer (initiated by the original holder)",
            description = "Scope: ticket:transfer:cancel. Cancels a pending transfer, restores the previous holder and ISSUED status, removes the pending transfer record, and publishes ticket.transfer.cancelled.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending transfer cancelled successfully."),
            @ApiResponse(responseCode = "404", description = "Ticket not found."),
            @ApiResponse(responseCode = "422", description = "Ticket is not pending transfer or no transfer history exists."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    public ResponseEntity<TicketResponse> cancelTicketTransfer(
            @PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.cancelTransfer(ticketId));
    }
}
