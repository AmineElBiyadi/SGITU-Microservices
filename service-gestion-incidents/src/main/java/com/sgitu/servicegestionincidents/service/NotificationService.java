package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.model.entity.Incident;

public interface NotificationService {

    void notifierActeurs(Incident incident);
    void notifierDeclarant(Long incidentId, String message);
}
