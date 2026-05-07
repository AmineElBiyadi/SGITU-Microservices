package com.ensate.billetterie.ticket.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class TransferRecordResponse {
    private String fromHolderId;
    private String toHolderId;
    private Instant transferredAt;
    private String reason;
}
