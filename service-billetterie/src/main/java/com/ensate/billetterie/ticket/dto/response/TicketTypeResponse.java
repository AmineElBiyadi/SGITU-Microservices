package com.ensate.billetterie.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class TicketTypeResponse {

    private String id;
    private String name;
    private String description;
    private double basePrice;
    private String currency;
    private boolean transferable;
    private boolean refundable;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
