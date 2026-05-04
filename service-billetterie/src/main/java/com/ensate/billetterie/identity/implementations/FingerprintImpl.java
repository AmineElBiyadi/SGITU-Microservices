package com.ensate.billetterie.identity.implementations;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class FingerprintImpl implements IdentityMethod {
    @Override
    public IdentityToken generateToken(IdentityContext identityContext) {
        String payloadToHash = identityContext.getHolderId() + ":" + identityContext.getRawPayload();
        String hash = Integer.toHexString(payloadToHash.hashCode() & 0x7fffffff);
        String tokenValue = "FP:" + hash;

        return IdentityToken.builder()
                .tokenValue(tokenValue)
                .methodType(IdentityMethodType.FINGERPRINT)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .metadata(Map.of("hash_algorithm", "SIMULATED_FP"))
                .build();
    }

    @Override
    public boolean verifyToken(IdentityToken identityToken, IdentityContext identityContext) {
        if (identityToken == null || identityToken.getTokenValue() == null) {
            return false;
        }
        
        String expectedPayloadToHash = identityContext.getHolderId() + ":" + identityContext.getRawPayload();
        String expectedHash = Integer.toHexString(expectedPayloadToHash.hashCode() & 0x7fffffff);
        String expectedTokenValue = "FP:" + expectedHash;
        
        return identityToken.getTokenValue().equals(expectedTokenValue) &&
               IdentityMethodType.FINGERPRINT.equals(identityToken.getMethodType());
    }
}
