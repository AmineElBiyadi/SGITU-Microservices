package ma.sgitu.payment.enums;

public enum FailureReason {
    INSUFFICIENT_BALANCE,
    ACCOUNT_BLOCKED,
    ACCOUNT_EXPIRED,
    ACCOUNT_NOT_ACTIVE,
    INVALID_TOKEN,
    UNAUTHORIZED_TOKEN
}