package com.serviceabonnement.controller;

import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.service.PlanAbonnementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanAbonnementController {

    private final PlanAbonnementService planService;

    @GetMapping
    public ResponseEntity<List<PlanAbonnement>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanAbonnement> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @PostMapping
    public ResponseEntity<PlanAbonnement> createPlan(@RequestBody PlanAbonnement plan) {
        return ResponseEntity.ok(planService.createPlan(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanAbonnement> updatePlan(@PathVariable Long id, @RequestBody PlanAbonnement plan) {
        return ResponseEntity.ok(planService.updatePlan(id, plan));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
