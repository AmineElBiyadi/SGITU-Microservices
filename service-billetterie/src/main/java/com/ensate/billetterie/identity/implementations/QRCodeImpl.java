package com.ensate.billetterie.identity.implementations;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

public class QRCodeImpl implements IdentityMethod {
    @Override
    public IdentityToken generateToken(IdentityContext identityContext) {
        String payload = identityContext.getHolderId() + ":" + identityContext.getEventId();
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes());
        String tokenValue = "QR:" + encoded;

        return IdentityToken.builder()
                .tokenValue(tokenValue)
                .methodType(IdentityMethodType.QR_CODE)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .metadata(Map.of("encoded_payload", encoded))
                .build();
    }

    @Override
    public boolean verifyToken(IdentityToken identityToken, IdentityContext identityContext) {
        if (identityToken == null || identityToken.getTokenValue() == null) {
            return false;
        }
        
        String expectedPayload = identityContext.getHolderId() + ":" + identityContext.getEventId();
        String expectedEncoded = Base64.getEncoder().encodeToString(expectedPayload.getBytes());
        String expectedTokenValue = "QR:" + expectedEncoded;
        
        return identityToken.getTokenValue().equals(expectedTokenValue) && 
               IdentityMethodType.QR_CODE.equals(identityToken.getMethodType());
    }
}
