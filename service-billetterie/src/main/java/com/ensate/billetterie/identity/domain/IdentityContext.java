package com.ensate.billetterie.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityContext {
    private String holderId;
    private String eventId;
    private String rawPayload;
}
