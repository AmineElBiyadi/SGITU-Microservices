package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.request.SignalementRequestDTO;
import com.sgitu.servicegestionincidents.dto.response.*;
import com.sgitu.servicegestionincidents.model.enums.StatutIncident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IncidentServiceImpl implements IncidentService {

    // TODO

    @Override
    public SignalementResponseDTO signalerIncident(SignalementRequestDTO request) {
        // TODO
        return null;
    }

    @Override
    public IncidentResponseDTO consulterIncident(Long id) {
        // TODO
        return null;
    }

    @Override
    public List<ActionDTO> consulterSuivi(Long incidentId) {
        // TODO
        return null;
    }

    @Override
    public List<IncidentResponseDTO> filtrerIncidents(Map<String, Object> criteres) {
        // TODO
        return null;
    }

    @Override
    public void cloturerIncident(Long id, String motif) {
        // TODO
    }

    @Override
    public void escaladerIncident(Long id, String motif) {
        // TODO
    }

    @Override
    public void affecterResponsable(Long id, Long responsableId) {
        // TODO
    }

    @Override
    public void mettreAJourStatut(Long id, StatutIncident statut) {
        // TODO
    }
}
