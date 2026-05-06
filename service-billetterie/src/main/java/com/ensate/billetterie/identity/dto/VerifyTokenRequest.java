package com.ensate.billetterie.identity.dto;

import com.ensate.billetterie.identity.domain.IdentityToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTokenRequest {
    private IdentityToken token;
    private String holderId;
    private String eventId;
    private Map<String, Object> rawPayload;
}
