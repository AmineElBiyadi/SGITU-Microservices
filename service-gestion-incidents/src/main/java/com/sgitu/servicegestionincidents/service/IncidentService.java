package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.request.SignalementRequestDTO;
import com.sgitu.servicegestionincidents.dto.response.*;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;

import java.util.List;
import java.util.Map;

public interface IncidentService {

    SignalementResponseDTO signalerIncident(SignalementRequestDTO request, Long declarantId);
    IncidentResponseDTO consulterIncident(Long id);
    List<ActionDTO> consulterSuivi(Long incidentId);
    List<IncidentResponseDTO> filtrerIncidents(Map<String, Object> criteres);
    void cloturerIncident(Long id, String motif, Long auteurId);
    void escaladerIncident(Long id, String motif, Long auteurId);
    void demanderEscalade(Long id, String motif, Long auteurId);
    void refuserEscalade(Long id, String motifRefus, Long auteurId);
    void affecterResponsable(Long id, Long responsableId, Long auteurId);
    void mettreAJourStatut(Long id, StatutIncident statut, Long auteurId);
    void annulerIncident(Long id, String motif, Long auteurId);
    void ajouterRenfort(Long incidentId, Long agentId, Long auteurId);
    List<IncidentResponseDTO> obtenirTousLesIncidents();
    List<IncidentResponseDTO> obtenirIncidentsNonEscalades();
    List<IncidentResponseDTO> obtenirIncidentsEscalades();
    List<IncidentResponseDTO> obtenirIncidentsAffectes(Long userId);
    List<IncidentResponseDTO> obtenirDemandesEscalades();
    List<IncidentResponseDTO> obtenirMesSignalements(Long userId);
    List<RenfortDTO> obtenirRenforts(Long incidentId);
}
