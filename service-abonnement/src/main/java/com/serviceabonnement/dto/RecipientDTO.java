package com.serviceabonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecipientDTO {
    private String userId;
    private String email;
    private String phone;
    private String deviceToken;
}
