package com.ensate.billetterie.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DashboardResponse {

    // ── Ticket counts by status ──────────────────────────────────────
    private long totalTickets;
    private long issuedCount;
    private long redeemedCount;
    private long cancelledCount;
    private long refundedCount;
    private long transferredCount;
    private long expiredCount;
    private long flaggedCount;

    // ── Revenue ──────────────────────────────────────────────────────
    private double totalRevenue;
    private double refundedRevenue;
    private double netRevenue;
    private String currency;

    // ── Fraud / flags ────────────────────────────────────────────────
    private long openFraudCases;
    private long confirmedFraudCases;
    private long resolvedFlagCases;

    // ── Transfers ────────────────────────────────────────────────────
    private long transfersInitiated;
    private long transfersCompleted;
    private long transfersCancelled;

    // ── Meta ─────────────────────────────────────────────────────────
    /** Timestamp at which this snapshot was computed. */
    private Instant generatedAt;
}
