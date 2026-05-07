package com.sgitu.g4.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class HoraireRequest {

	@NotNull
	private Long trajetId;
	private Long arretId;

	@NotNull
	private LocalTime heurePassage;
	private Integer jourSemaine;
	private LocalDate validFrom;
	private LocalDate validTo;

	@Size(max = 500)
	private String libelle;
}
