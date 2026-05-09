package com.sgitu.g4.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

	@Min(1)
	@Max(7)
	@Schema(
			description = "Jour de la semaine (ISO-8601 / java.time.DayOfWeek) : 1 = lundi … 7 = dimanche. "
					+ "Omettre ou null = horaire valable tous les jours.")
	private Integer jourSemaine;

	@Schema(description = "Début de période de validité (inclus). Null = pas de borne de début.")
	private LocalDate validFrom;

	@Schema(description = "Fin de période de validité (inclus). Null = pas de borne de fin.")
	private LocalDate validTo;

	@Size(max = 500)
	@Schema(
			description = "Libellé métier optionnel pour opérateurs ou affichage (ex. « scolaire », « vacances », « renfort 7h-9h »). "
					+ "N’alimente pas le calcul des horaires : information humaine.")
	private String libelle;
}
