package com.ensate.billetterie.ticket.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForceRefundRequest {

    /**
     * Reason for bypassing the normal refund flow (e.g. "system outage caused
     * duplicate charge", "admin override after customer dispute").
     */
    @NotBlank(message = "reason is required")
    private String reason;

    /** ID of the admin authorising the force-refund. */
    @NotBlank(message = "refundedBy is required")
    private String refundedBy;
}
