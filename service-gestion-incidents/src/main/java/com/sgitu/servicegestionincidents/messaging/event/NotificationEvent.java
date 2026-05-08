package com.sgitu.servicegestionincidents.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private Long destinataireId;
    private String sujet;
    private String message;
    private String canal;          // EMAIL, SMS, IN-APP, PUSH
    private String referenceIncident;
    private LocalDateTime dateEvenement;
}
