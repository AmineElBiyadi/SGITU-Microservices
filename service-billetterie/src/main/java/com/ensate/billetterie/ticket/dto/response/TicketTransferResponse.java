package com.ensate.billetterie.ticket.dto.response;

public record TicketTransferResponse(
        TicketResponse originalTicket,   // old holder — status: TRANSFER_PENDING
        TicketResponse newHolderTicket   // new holder — status: CREATED/PENDING
) {}
