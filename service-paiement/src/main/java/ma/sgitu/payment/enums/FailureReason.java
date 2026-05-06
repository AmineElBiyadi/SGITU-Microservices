package ma.sgitu.payment.enums;

public enum FailureReason {
    INSUFFICIENT_BALANCE,
    ACCOUNT_BLOCKED,
    ACCOUNT_EXPIRED,
    ACCOUNT_NOT_ACTIVE,
    INVALID_TOKEN,
    CARD_EXPIRED,
    CARD_BLOCKED,
    UNAUTHORIZED_TOKEN,
    INVALID_CVV,
    REFUND_AMOUNT_INVALID,    // pour remboursement
    MAX_ATTEMPTS_REACHED
}