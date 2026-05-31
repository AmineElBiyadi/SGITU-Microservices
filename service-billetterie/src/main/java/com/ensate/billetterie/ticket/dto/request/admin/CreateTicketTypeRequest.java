package com.ensate.billetterie.ticket.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class CreateTicketTypeRequest {

    /** Unique identifier name for this ticket type (e.g. "VIP", "STANDARD"). */
    @NotBlank(message = "name is required")
    private String name;

    /** Human-readable description shown to end-users and staff. */
    private String description;

    /** Base price associated with this ticket type. */
    @PositiveOrZero(message = "basePrice must be zero or positive")
    private double basePrice;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-character ISO-4217 code")
    private String currency;

    /** Whether tickets of this type are transferable between holders. */
    private boolean transferable;

    /** Whether tickets of this type are refundable. */
    private boolean refundable;

    /** Arbitrary key-value configuration (e.g. max_uses, zone, perks). */
    private Map<String, Object> metadata;
}
