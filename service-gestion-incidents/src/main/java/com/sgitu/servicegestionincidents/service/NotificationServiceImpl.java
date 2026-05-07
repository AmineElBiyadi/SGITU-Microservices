package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.model.entity.Incident;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void notifierActeurs(Incident incident) {
        // TODO
    }

    @Override
    public void notifierDeclarant(Long incidentId, String message) {
        // TODO
    }
}
