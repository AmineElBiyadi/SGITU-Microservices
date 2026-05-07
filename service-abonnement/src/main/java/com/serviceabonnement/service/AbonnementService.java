package com.serviceabonnement.service;

import com.serviceabonnement.entity.Abonnement;
import com.serviceabonnement.entity.Renouvellement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AbonnementService {
    // Actions Abonnés
    Abonnement souscrire(Long userId, Long planId);
    Abonnement renouveler(Long abonnementId);
    Abonnement renouvelerManuel(Long abonnementId);
    void demanderAnnulation(Long abonnementId);
    void desactiver(Long abonnementId, int jours);
    Abonnement toggleAutoRenouvellement(Long abonnementId, boolean enable);
    
    // Actions Superviseurs
    void suspendre(Long abonnementId, String motif);
    void forcerAnnulation(Long abonnementId, String motif);
    void forcerRenouvellement(Long abonnementId);
    
    // Consultation
    Abonnement getAbonnementById(Long id);
    List<Abonnement> getAbonnementsByUtilisateur(Long userId);
    Page<Abonnement> getFullHistory(Long userId, Pageable pageable);
    Abonnement getActif(Long userId);
    List<Renouvellement> getHistoriquePaiements(Long abonnementId);

    // Callbacks
    void confirmerPaiement(String transactionId);
    void confirmerRemboursement(String transactionId);
}
