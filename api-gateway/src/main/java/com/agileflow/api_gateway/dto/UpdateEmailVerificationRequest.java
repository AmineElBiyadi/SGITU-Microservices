package com.agileflow.api_gateway.dto;

import lombok.Data;

@Data
public class UpdateEmailVerificationRequest {
    private boolean emailVerified;
}
