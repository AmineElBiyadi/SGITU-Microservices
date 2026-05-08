package com.serviceabonnement.service;

import com.serviceabonnement.entity.Abonnement;
import java.util.List;

public interface AbonnementService {
    // Actions Abonnés
    Abonnement souscrire(Long userId, Long planId);
    Abonnement renouveler(Long abonnementId);
    void demanderAnnulation(Long abonnementId);
    void desactiver(Long abonnementId, int jours);
    
    // Actions Superviseurs
    void suspendre(Long abonnementId, String motif);
    void forcerAnnulation(Long abonnementId, String motif);
    void forcerRenouvellement(Long abonnementId);
    
    // Consultation
    Abonnement getAbonnementById(Long id);
    List<Abonnement> getAbonnementsByUtilisateur(Long userId);
    Abonnement getActif(Long userId);

    // Callbacks
    void confirmerPaiement(String transactionId);
    void confirmerRemboursement(String transactionId);
}
