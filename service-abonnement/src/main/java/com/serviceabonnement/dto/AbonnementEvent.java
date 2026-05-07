package com.serviceabonnement.dto;

import com.serviceabonnement.enums.TypeNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AbonnementEvent {
    private Long abonnementId;
    private Long userId;
    private TypeNotification type;
    private LocalDateTime timestamp;
    private String motif;
}
