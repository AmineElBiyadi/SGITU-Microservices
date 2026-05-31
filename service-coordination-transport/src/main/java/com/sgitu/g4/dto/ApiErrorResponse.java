package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Erreur API normalisée")
public class ApiErrorResponse {

	private int status;
	private String error;
	private String message;
	private String path;
	private Instant timestamp;
	private List<FieldErrorDto> fieldErrors;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FieldErrorDto {
		private String field;
		private String message;
		private Object rejectedValue;
	}
}
