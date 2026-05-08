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
    void cloturerIncident(Long id, String motif);
    void escaladerIncident(Long id, String motif);
    void affecterResponsable(Long id, Long responsableId);
    void mettreAJourStatut(Long id, StatutIncident statut);
    void annulerIncident(Long id, String motif);
}
