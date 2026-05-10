package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneResponse {

	private Long id;
	private String code;
	private String nom;
	private String description;
	private boolean active;
	private Instant createdAt;
	private Instant updatedAt;
}
