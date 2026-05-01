package com.ensate.billetterie.identity.interfaces;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityToken;

public interface IdentityMethod {
    IdentityToken generateToken(IdentityContext identityContext);
    boolean verifyToken(IdentityToken identityToken, IdentityContext identityContext);
}
