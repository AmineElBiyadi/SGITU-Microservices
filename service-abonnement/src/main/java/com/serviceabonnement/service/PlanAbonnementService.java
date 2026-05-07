package com.serviceabonnement.service;

import com.serviceabonnement.entity.PlanAbonnement;
import com.serviceabonnement.repository.PlanAbonnementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanAbonnementService {

    private final PlanAbonnementRepository planRepository;

    public List<PlanAbonnement> getAllPlans() {
        return planRepository.findAll();
    }

    public PlanAbonnement getPlanById(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new RuntimeException("Plan non trouvé"));
    }

    public PlanAbonnement createPlan(PlanAbonnement plan) {
        return planRepository.save(plan);
    }

    public PlanAbonnement updatePlan(Long id, PlanAbonnement planDetails) {
        PlanAbonnement plan = getPlanById(id);
        plan.setNomPlan(planDetails.getNomPlan());
        plan.setPrix(planDetails.getPrix());
        plan.setDuree(planDetails.getDuree());
        plan.setCategorie(planDetails.getCategorie());
        plan.setTransportType(planDetails.getTransportType());
        plan.setEstActif(planDetails.getEstActif());
        return planRepository.save(plan);
    }

    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }
}
