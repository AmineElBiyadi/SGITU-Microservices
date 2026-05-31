package com.ensate.billetterie.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketRefundRequest {

    /**
     * Reason for the refund request (e.g. "event cancelled", "duplicate purchase").
     * Stored on the ticket and forwarded to any downstream billing service via Kafka.
     */
    @NotBlank(message = "reason is required")
    private String reason;

    /**
     * ID of the user or system actor requesting the refund.
     * Optional — defaults to the authenticated principal when security is wired in.
     */
    private String refundedBy;
}
