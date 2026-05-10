package com.sgitu.g4.dto;

import com.sgitu.g4.entity.DirectionTrajet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Création / mise à jour trajet")
public class TrajetRequest {

	@NotNull
	private Long ligneId;

	@NotBlank
	@Size(max = 64)
	private String code;

	@NotBlank
	@Size(max = 200)
	private String nom;

	@NotNull
	private DirectionTrajet sens;

	@NotNull
	private Boolean actif;

	@Valid
	private List<TrajetArretSequenceItem> arretSequence;
}
