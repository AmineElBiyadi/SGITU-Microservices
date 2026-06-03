package com.ensate.billetterie.identity.domain;

public enum IdentityMethodType {
    QR_CODE,
    QR_CODE_EXTERNAL,       // Implémentation réelle via ZXing
    FINGERPRINT,
    FINGERPRINT_EXTERNAL,   // Implémentation réelle via SourceAFIS
    FACE_ID
}

