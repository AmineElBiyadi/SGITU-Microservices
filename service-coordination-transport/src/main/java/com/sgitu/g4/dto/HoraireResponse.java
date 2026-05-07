package com.sgitu.g4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoraireResponse {

	private Long id;
	private Long trajetId;
	private Long arretId;
	private LocalTime heurePassage;
	private Integer jourSemaine;
	private LocalDate validFrom;
	private LocalDate validTo;
	private String libelle;
	private Instant createdAt;
	private Instant updatedAt;
}
