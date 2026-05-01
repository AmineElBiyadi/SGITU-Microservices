package com.ensate.billetterie.identity.implementations;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;

public class FaceIDImpl implements IdentityMethod {
    @Override
    public IdentityToken generateToken(IdentityContext identityContext) {
        return null;
    }

    @Override
    public boolean verifyToken(IdentityToken identityToken, IdentityContext identityContext) {
        return false;
    }
}
