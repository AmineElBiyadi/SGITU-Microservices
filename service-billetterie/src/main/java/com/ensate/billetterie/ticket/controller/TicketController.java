package com.ensate.billetterie.ticket.controller;


import com.ensate.billetterie.ticket.dto.request.*;
import com.ensate.billetterie.ticket.dto.response.TicketResponse;
import com.ensate.billetterie.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Normal User – Ticket Management", description = "Normal User Operations for managing ticket operations")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;



    @GetMapping
    @Operation(summary = "Get all tickets")
    public ResponseEntity<List<TicketResponse>> getTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get a single ticket by its ID")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all tickets associated with a particular user")
    public ResponseEntity<List<TicketResponse>> getUserTicketHistory(@PathVariable String userId) {
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }



    @PostMapping
    @Operation(summary = "Create a new paperless ticket")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest createTicketRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ticketService.createTicket(createTicketRequest));
    }



    @PostMapping("/{ticketId}/validate")
    @Operation(summary = "Validate (redeem) a ticket")
    public ResponseEntity<TicketResponse> validateTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody ValidateTicketRequest validateTicketRequest) {
        return ResponseEntity.ok(ticketService.validateTicket(ticketId, validateTicketRequest));
    }


    @PostMapping("/{ticketId}/pay")
    @Operation(summary = "Pay for ticket")
    public ResponseEntity<TicketResponse> payTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        return ResponseEntity.ok(ticketService.payTicket(ticketId, paymentRequest));
    }

    @PostMapping("/{ticketId}/cancel")
    @Operation(summary = "Cancel a ticket")
    public ResponseEntity<TicketResponse> cancelTicket(
            @PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.cancelTicket(ticketId));
    }

    @PostMapping("/{ticketId}/refund")
    @Operation(summary = "Request a refund for a cancelled ticket")
    public ResponseEntity<TicketResponse> refundTicket(
            @PathVariable String ticketId) {
        return ResponseEntity.ok(ticketService.refundTicket(ticketId));
    }



    @PostMapping("/{ticketId}/transfer")
    @Operation(summary = "Initiate a ticket transfer to a new holder")
    public ResponseEntity<TicketResponse> transferTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketTransferRequest ticketTransferRequest) {
        return ResponseEntity.ok(ticketService.transferTicket(ticketId, ticketTransferRequest));
    }

    @PostMapping("/{ticketId}/transfer/accept")
    @Operation(summary = "Accept a pending ticket transfer")
    public ResponseEntity<TicketResponse> acceptTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketAcceptRequest ticketAcceptRequest) {
        return ResponseEntity.ok(ticketService.acceptTransfer(ticketId, ticketAcceptRequest));
    }

    @PostMapping("/{ticketId}/transfer/reject")
    @Operation(summary = "Reject a pending ticket transfer")
    public ResponseEntity<TicketResponse> rejectTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketAcceptRequest ticketAcceptRequest) {
        return ResponseEntity.ok(ticketService.rejectTransfer(ticketId, ticketAcceptRequest));
    }


    @PostMapping("/{ticketId}/transfer/cancel")
    @Operation(summary = "Cancel a pending ticket transfer (initiated by the original holder)")
    public ResponseEntity<TicketResponse> cancelTicketTransfer(
            @PathVariable String ticketId,
            @Valid @RequestBody TicketCancelRequest ticketCancelRequest) {
        return ResponseEntity.ok(ticketService.cancelTransfer(ticketId, ticketCancelRequest));
    }
}
