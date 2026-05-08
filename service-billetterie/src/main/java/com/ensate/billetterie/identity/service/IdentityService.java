package com.ensate.billetterie.identity.service;

import com.ensate.billetterie.identity.domain.IdentityContext;
import com.ensate.billetterie.identity.domain.IdentityToken;
import com.ensate.billetterie.identity.dto.IssueTokenRequest;
import com.ensate.billetterie.identity.dto.IssueTokenResponse;
import com.ensate.billetterie.identity.dto.VerifyTokenRequest;
import com.ensate.billetterie.identity.factories.IdentityMethodFactory;
import com.ensate.billetterie.identity.interfaces.IdentityMethod;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {

    public IssueTokenResponse issue(IssueTokenRequest request) {
        IdentityContext ctx = IdentityContext.builder()
                .holderId(request.getHolderId())
                .eventId(request.getEventId())
                .rawPayload(request.getRawPayload())
                .build();

        IdentityMethod identityMethod = IdentityMethodFactory.create(request.getMethodType());
        IdentityToken token = identityMethod.generateToken(ctx);

        return IssueTokenResponse.builder()
                .tokenValue(token.getTokenValue())
                .methodType(token.getMethodType())
                .issuedAt(token.getIssuedAt())
                .expiresAt(token.getExpiresAt())
                .metadata(token.getMetadata())
                .build();
    }

    public boolean verify(VerifyTokenRequest request) {
        if (request == null || request.getToken() == null || request.getToken().getMethodType() == null) {
            return false;
        }

        IdentityContext ctx = IdentityContext.builder()
                .holderId(request.getHolderId())
                .eventId(request.getEventId())
                .rawPayload(request.getRawPayload())
                .build();

        IdentityMethod identityMethod = IdentityMethodFactory.create(request.getToken().getMethodType());
        return identityMethod.verifyToken(request.getToken(), ctx);
    }
}
