package com.ensate.billetterie.ticket.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FraudConfirmRequest {

    /**
     * Detailed explanation of the confirmed fraud (e.g. cloned QR code,
     * stolen identity, multiple redemptions).
     */
    @NotBlank(message = "fraudReason is required")
    private String fraudReason;

    /** ID of the admin confirming the fraud finding. */
    @NotBlank(message = "confirmedBy is required")
    private String confirmedBy;

    /**
     * When true, immediately blacklists the ticket holder's account in
     * addition to cancelling the ticket.
     */
    private boolean blacklistHolder;
}
