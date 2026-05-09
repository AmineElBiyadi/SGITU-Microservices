package com.ensate.billetterie.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityContext {
    private String holderId;
    private String eventId;
    private Map<String, Object> rawPayload;
}
