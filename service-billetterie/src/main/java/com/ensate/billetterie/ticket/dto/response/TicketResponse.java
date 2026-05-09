package com.ensate.billetterie.ticket.dto.response;

import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.ticket.domain.enums.TicketClass;
import com.ensate.billetterie.ticket.domain.enums.TicketStatus;
import com.ensate.billetterie.ticket.domain.enums.TicketType;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class TicketResponse {

    private String id;
    private String tripId;
    private double price;
    private String currency;
    private TicketType ticketType;
    private TicketClass ticketClass;
    private String holderId;
    private String tokenValue;
    private IdentityMethodType identityMethod;
    private TicketStatus status;
    private List<TransferRecordResponse> transferHistory;
    private Map<String, Object> metadata;
    private Instant expiresAt;
    private Instant flaggedAt;
    private Instant redeemedAt;
    private Instant refundedAt;
    private Instant cancelledAt;
    private Instant transferredAt;
    private Instant issuedAt;
    private Instant updatedAt;
}
