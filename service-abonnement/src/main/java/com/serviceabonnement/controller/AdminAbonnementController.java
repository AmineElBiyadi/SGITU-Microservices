package com.serviceabonnement.controller;

import com.serviceabonnement.service.AbonnementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/abonnements/admin")
@RequiredArgsConstructor
public class AdminAbonnementController {

    private final AbonnementService abonnementService;

    @PostMapping("/{id}/suspendre")
    public ResponseEntity<Void> suspendre(@PathVariable Long id, @RequestParam String motif) {
        abonnementService.suspendre(id, motif);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/forcer-annulation")
    public ResponseEntity<Void> forcerAnnulation(@PathVariable Long id, @RequestParam String motif) {
        abonnementService.forcerAnnulation(id, motif);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/forcer-renouvellement")
    public ResponseEntity<Void> forcerRenouvellement(@PathVariable Long id) {
        abonnementService.forcerRenouvellement(id);
        return ResponseEntity.ok().build();
    }
}
