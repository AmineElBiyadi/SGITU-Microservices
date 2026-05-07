package com.ensate.billetterie.identity.dto;

import com.ensate.billetterie.identity.domain.IdentityMethodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTokenRequest {
    private IdentityMethodType methodType;
    private String holderId;
    private String eventId;
    private String rawPayload;
}
