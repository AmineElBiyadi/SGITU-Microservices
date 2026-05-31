package com.ensate.billetterie.ticket.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCancelRequest {

    /**
     * Reason for the forced cancellation. Stored in the audit trail and
     * forwarded via the TICKET_CANCELLED Kafka event.
     */
    @NotBlank(message = "reason is required")
    private String reason;

    /** ID of the admin performing the cancellation. */
    @NotBlank(message = "cancelledBy is required")
    private String cancelledBy;
}
