package com.ensate.billetterie.ticket.dto.request.admin;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateTicketTypeRequest {

    /** Updated display name. Leave null to keep the existing value. */
    private String name;

    /** Updated description. Leave null to keep the existing value. */
    private String description;

    @PositiveOrZero(message = "basePrice must be zero or positive")
    private Double basePrice;

    @Size(min = 3, max = 3, message = "currency must be a 3-character ISO-4217 code")
    private String currency;

    private Boolean transferable;
    private Boolean refundable;

    private Map<String, Object> metadata;
}
