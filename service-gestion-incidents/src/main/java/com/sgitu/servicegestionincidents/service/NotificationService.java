package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.model.entity.Incident;

public interface NotificationService {

    void envoyerConfirmation(Incident incident);
    void envoyerChangementStatut(Incident incident, String ancienStatut);
    void envoyerAlerteIoT(Incident incident);
    void envoyerEscalade(Incident incident, String motif);
}
