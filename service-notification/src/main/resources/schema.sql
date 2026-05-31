-- ═══════════════════════════════════════════════════════════════════════════
--  Script SQL — notifications_db · G5 Microservice Notifications
-- ═══════════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS notifications_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE notifications_db;

CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    notification_id     VARCHAR(100)    NOT NULL,
    user_id             VARCHAR(100)    NOT NULL,
    source_service      VARCHAR(100)    NULL,
    event_type          VARCHAR(100)    NULL,
    type                ENUM('EMAIL', 'SMS', 'PUSH') NOT NULL,
    channel             VARCHAR(50)     NOT NULL,
    subject             VARCHAR(255)    NULL,
    content             TEXT            NULL,
    recipient           VARCHAR(255)    NOT NULL,
    priority            VARCHAR(20)     DEFAULT 'NORMAL',
    status              ENUM('PENDING', 'SENT', 'FAILED') NOT NULL,
    sent_at             DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count         INT             NOT NULL DEFAULT 0,
    provider            VARCHAR(100)    NULL,
    email               VARCHAR(255)    NULL,
    phone               VARCHAR(50)     NULL,
    device_token        VARCHAR(255)    NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_id (notification_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ─────────────────────────────────────────────────────────────────────────────
--  Données de test (seed)
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO notifications (notification_id, user_id, source_service, event_type, type, channel, subject, content, recipient, status, sent_at)
VALUES
    ('test-001', 'U10', 'G6_PAYMENT', 'PAYMENT_SUCCESS', 'EMAIL', 'EMAIL', 'Paiement confirmé', 'Votre paiement de 150 MAD a été validé.', 'client@example.com', 'SENT', NOW()),
    ('test-002', 'U123', 'G9_INCIDENT', 'INCIDENT_CONFIRMATION', 'SMS', 'SMS', NULL, 'Votre incident INC-2024 a été enregistré.', '+212600000000', 'FAILED', NULL);
