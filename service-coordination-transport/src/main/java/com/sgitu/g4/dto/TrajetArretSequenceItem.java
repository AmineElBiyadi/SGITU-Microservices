package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Ordre d'un arrêt sur un trajet")
public class TrajetArretSequenceItem {

	@NotNull
	private Long arretId;

	@NotNull
	@Positive
	private Integer sequenceOrder;
}
