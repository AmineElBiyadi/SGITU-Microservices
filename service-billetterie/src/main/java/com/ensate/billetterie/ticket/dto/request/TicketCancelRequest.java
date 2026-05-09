package com.ensate.billetterie.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketCancelRequest {

    /** Human-readable reason for the cancellation. Required for audit trail. */
    @NotBlank(message = "reason is required")
    private String reason;

    /**
     * ID of the user or system actor initiating the cancellation.
     * Optional — defaults to the authenticated principal when security is wired in.
     */
    private String cancelledBy;
}
