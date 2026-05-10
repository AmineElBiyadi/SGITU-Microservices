package com.sgitu.g4.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class G9IncidentKafkaMessage {

	private String referenceIncident;
	private String type;
	private String statut;
	private String vehiculeId;
	private String ligneId;
	private String description;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private LocalDateTime timestamp;
}
