package com.sgitu.g4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArretResponse {

	private Long id;
	private String code;
	private String nom;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private Long ligneId;
	private Instant createdAt;
	private Instant updatedAt;
}
