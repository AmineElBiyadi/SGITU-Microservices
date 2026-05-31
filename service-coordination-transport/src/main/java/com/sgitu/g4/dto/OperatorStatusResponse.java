package com.sgitu.g4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorStatusResponse {

	private String mode;
	private long missionsActives;
	private Map<String, String> integrations;
	private Instant generatedAt;
}
