package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.model.entity.Incident;

public interface NotificationService {

    void envoyerConfirmation(Incident incident);
    void envoyerChangementStatut(Incident incident, String ancienStatut);
    void envoyerAlerteDispatchers(Incident incident);
    void envoyerEscalade(Incident incident, String motif);
    void envoyerAssignation(Incident incident);
    void envoyerAssignationRenfort(Incident incident, Long agentId);
}
