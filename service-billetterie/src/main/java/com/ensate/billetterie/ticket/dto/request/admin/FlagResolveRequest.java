package com.ensate.billetterie.ticket.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlagResolveRequest {

    /**
     * Resolution note written by the admin after reviewing the flagged ticket.
     * Stored in the audit trail.
     */
    @NotBlank(message = "resolutionNote is required")
    private String resolutionNote;

    /** ID of the admin resolving the flag. */
    @NotBlank(message = "resolvedBy is required")
    private String resolvedBy;
}
