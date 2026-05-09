package com.sgitu.servicegestionincidents.controller;

import com.sgitu.servicegestionincidents.dto.response.RapportDTO;
import com.sgitu.servicegestionincidents.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports et Statistiques", description = "APIs pour générer des rapports")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping
    @Operation(summary = "Générer un rapport par période")
    public ResponseEntity<RapportDTO> genererRapport(
            @RequestParam(defaultValue = "mois") String periode) {
        RapportDTO rapport = rapportService.genererRapport(periode);
        return ResponseEntity.ok(rapport);
    }

    @GetMapping("/tableau-bord")
    @Operation(summary = "Consulter le tableau de bord")
    public ResponseEntity<Map<String, Object>> consulterTableauBord() {
        Map<String, Object> tableauBord = rapportService.genererTableauBord();
        return ResponseEntity.ok(tableauBord);
    }
}
