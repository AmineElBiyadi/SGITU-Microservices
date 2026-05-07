package com.ensate.billetterie.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketAcceptRequest {

    /**
     * The user ID of the person accepting or rejecting the transfer.
     * The service layer validates that this matches the ticket's current
     * {@code holderId} (i.e. the pending recipient), preventing a third
     * party from acting on someone else's transfer.
     */
    @NotBlank(message = "acceptingUserId is required")
    private String acceptingUserId;

    /**
     * Optional reason — particularly useful on rejection so the original
     * holder understands why the transfer was declined.
     */
    private String reason;
}
