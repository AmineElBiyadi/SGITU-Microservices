package com.ensate.billetterie.identity.dto;

import com.ensate.billetterie.identity.domain.IdentityMethodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTokenResponse {
    private String tokenValue;
    private IdentityMethodType methodType;
    private Instant issuedAt;
    private Instant expiresAt;
    private Map<String, String> metadata;
}
