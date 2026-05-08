package com.ensate.billetterie.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketTransferRequest {

    /**
     * The user ID of the intended new ticket holder.
     * Must resolve to a valid user in the system; validation against the
     * user service can be added as a service-layer guard if needed.
     */
    @NotBlank(message = "newHolderId is required")
    private String newHolderId;

    /**
     * Reason for the transfer (e.g. "gifting to a friend", "sold ticket").
     * Stored in the TransferRecord for audit purposes.
     */
    @NotBlank(message = "reason is required")
    private String reason;
}
