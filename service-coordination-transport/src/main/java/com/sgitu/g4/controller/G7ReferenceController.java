package com.sgitu.g4.controller;

import com.sgitu.g4.dto.ArretResponse;
import com.sgitu.g4.dto.HoraireResponse;
import com.sgitu.g4.dto.LigneResponse;
import com.sgitu.g4.dto.TrajetResponse;
import com.sgitu.g4.service.ArretService;
import com.sgitu.g4.service.HoraireService;
import com.sgitu.g4.service.LigneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class G7ReferenceController {

	private final LigneService ligneService;
	private final HoraireService horaireService;
	private final ArretService arretService;

	@GetMapping("/lignes")
	public List<LigneResponse> lignesActives() {
		return ligneService.findActives();
	}

	@GetMapping("/lignes/{id}/trajet")
	public List<TrajetResponse> trajetsLigne(@PathVariable("id") Long ligneId) {
		return ligneService.trajetsForLigne(ligneId);
	}

	@GetMapping("/lignes/{id}/horaires")
	public List<HoraireResponse> horairesLigne(@PathVariable("id") Long ligneId) {
		return horaireService.findByLigne(ligneId);
	}

	@GetMapping("/arrets")
	public List<ArretResponse> arrets() {
		return arretService.findAll();
	}

	@GetMapping("/arrets/{id}")
	public ArretResponse arret(@PathVariable("id") Long arretId) {
		return arretService.findById(arretId);
	}
}
