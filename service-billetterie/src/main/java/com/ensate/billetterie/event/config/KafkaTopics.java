package com.ensate.billetterie.event.config;

public class KafkaTopics {
    public static final String TICKET_ISSUED = "ticket.issued";
    public static final String TICKET_VALIDATED = "ticket.validated";
    public static final String TICKET_FLAGGED = "ticket.flagged";
    public static final String TICKET_PAYMENT_FAILED = "ticket.payment.failed";
    public static final String TICKET_PAYMENT_SUCCESS = "ticket.payment.success";
    public static final String TICKET_FLAG_REVIEWED = "ticket.flag.reviewed";
    public static final String TICKET_CANCELLED = "ticket.cancelled";
    public static final String TICKET_TRANSFER_INITIATED = "ticket.transfer.initiated";
    public static final String TICKET_TRANSFER_COMPLETED = "ticket.transfer.completed";
    public static final String TICKET_TRANSFER_CANCELLED = "ticket.transfer.cancelled";
    public static final String TICKET_TRANSFER_REJECTED = "ticket.transfer.rejected";
    public static final String TICKET_REFUND_REQUESTED = "ticket.refund.requested";
    public static final String TICKET_REFUND_COMPLETED = "ticket.refund.completed";
    public static final String TICKET_REFUND_CANCELLED = "ticket.refund.cancelled";
    public static final String TICKET_REFUNDED = "ticket.refunded";
    public static final String TICKET_EXPIRED = "ticket.expired";
}
