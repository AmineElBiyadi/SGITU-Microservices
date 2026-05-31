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
public class G4HealthResponse {

	private String status;
	private Instant checkedAt;
	private Map<String, String> components;
	private String buildVersion;
}
