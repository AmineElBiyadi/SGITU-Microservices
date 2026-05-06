package com.serviceabonnement.controller;

import com.serviceabonnement.dto.request.SouscriptionRequestDTO;
import com.serviceabonnement.dto.response.AbonnementResponseDTO;
import com.serviceabonnement.service.AbonnementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/abonnements")
@RequiredArgsConstructor
public class AbonnementController {

    private final AbonnementService abonnementService;

    @PostMapping("/souscrire")
    public ResponseEntity<com.serviceabonnement.entity.Abonnement> souscrire(
            @RequestParam Long userId, 
            @RequestParam Long planId) {
        return new ResponseEntity<>(abonnementService.souscrire(userId, planId), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.serviceabonnement.entity.Abonnement> getAbonnement(@PathVariable Long id) {
        return ResponseEntity.ok(abonnementService.getAbonnementById(id));
    }

    @GetMapping("/utilisateur/{userId}")
    public ResponseEntity<List<com.serviceabonnement.entity.Abonnement>> getAbonnementsByUtilisateur(@PathVariable Long userId) {
        return ResponseEntity.ok(abonnementService.getAbonnementsByUtilisateur(userId));
    }

    @GetMapping("/utilisateur/{userId}/actif")
    public ResponseEntity<com.serviceabonnement.entity.Abonnement> getActif(@PathVariable Long userId) {
        return ResponseEntity.ok(abonnementService.getActif(userId));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Void> demanderAnnulation(@PathVariable Long id) {
        abonnementService.demanderAnnulation(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id, @RequestParam int jours) {
        abonnementService.desactiver(id, jours);
        return ResponseEntity.ok().build();
    }
}
