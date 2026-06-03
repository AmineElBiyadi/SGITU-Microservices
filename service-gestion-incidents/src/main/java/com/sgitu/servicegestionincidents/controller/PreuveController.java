package com.sgitu.servicegestionincidents.controller;

import com.sgitu.servicegestionincidents.dto.request.PreuveUploadRequest;
import com.sgitu.servicegestionincidents.dto.response.PreuveUploadResponse;
import com.sgitu.servicegestionincidents.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents/preuves")
@Tag(name = "Gestion des Preuves", description = "APIs pour gérer le stockage et l'accès aux preuves multimédias")
public class PreuveController {

    private final StorageService storageService;

    public PreuveController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/request-upload")
    @PreAuthorize("hasAnyRole('ROLE_PASSENGER', 'ROLE_DRIVER', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR')")
    @Operation(summary = "Obtenir une URL pré-signée pour téléverser un fichier (PUT)")
    public ResponseEntity<PreuveUploadResponse> requestUploadUrl(@Valid @RequestBody PreuveUploadRequest request) {
        String uuid = UUID.randomUUID().toString();
        String extension = "";
        if (request.getNomFichier() != null && request.getNomFichier().contains(".")) {
            extension = request.getNomFichier().substring(request.getNomFichier().lastIndexOf("."));
        }
        String stockageKey = "preuves/" + uuid + extension;

        // URL valide pendant 15 minutes
        String uploadUrl = storageService.generatePreSignedUploadUrl(
                stockageKey, 
                request.getContentType(), 
                Duration.ofMinutes(15)
        );

        return ResponseEntity.ok(new PreuveUploadResponse(uploadUrl, stockageKey));
    }

    @GetMapping("/download-url")
    @PreAuthorize("hasAnyRole('ROLE_PASSENGER', 'ROLE_DRIVER', 'ROLE_TECHNICIAN', 'ROLE_DISPATCHER', 'ROLE_SUPERVISOR', 'ROLE_SECURITY', 'ROLE_MEDIC', 'ROLE_CLEANER')")
    @Operation(summary = "Obtenir l'URL de téléchargement temporaire (GET) d'un fichier privé")
    public ResponseEntity<String> getDownloadUrl(@RequestParam String stockageKey) {
        String downloadUrl = storageService.generatePreSignedDownloadUrl(stockageKey, Duration.ofMinutes(30));
        return ResponseEntity.ok(downloadUrl);
    }
}
