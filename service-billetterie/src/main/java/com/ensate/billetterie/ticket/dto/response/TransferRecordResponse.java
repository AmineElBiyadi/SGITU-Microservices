package com.ensate.billetterie.ticket.dto.response;

import lombok.Data;

import java.time.Instant;

@Data
public class TransferRecordResponse {
    private String fromHolder;
    private String toHolder;
    private Instant transferredAt;
    private String reason;
}
