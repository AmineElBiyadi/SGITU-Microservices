package com.ensate.billetterie.identity.implementations;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityMethodType;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class FaceIDImpl implements IdentityMethod {
    @Override
    public IdentityToken generateToken(IdentityContext identityContext) {
        String vectorSim = "VEC-" + (Math.abs(identityContext.getHolderId().hashCode()) & 0x7fffffff);
        String tokenValue = "FACE:" + vectorSim;

        return IdentityToken.builder()
                .tokenValue(tokenValue)
                .methodType(IdentityMethodType.FACE_ID)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .metadata(Map.of("confidence_score", "0.99"))
                .build();
    }

    @Override
    public boolean verifyToken(IdentityToken identityToken, IdentityContext identityContext) {
         if (identityToken == null || identityToken.getTokenValue() == null) {
            return false;
        }
        
        String expectedVectorSim = "VEC-" + (Math.abs(identityContext.getHolderId().hashCode()) & 0x7fffffff);
        String expectedTokenValue = "FACE:" + expectedVectorSim;
        
        return identityToken.getTokenValue().equals(expectedTokenValue) &&
               IdentityMethodType.FACE_ID.equals(identityToken.getMethodType());
    }
}
