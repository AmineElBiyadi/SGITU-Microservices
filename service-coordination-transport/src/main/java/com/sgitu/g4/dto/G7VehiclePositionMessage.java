package com.sgitu.g4.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class G7VehiclePositionMessage {
	private String vehiculeId;
	private String ligneId;
	private BigDecimal lat;
	@JsonProperty("long")
	private BigDecimal longitude;
	private BigDecimal vitesse;
	private Instant timestamp;
}
