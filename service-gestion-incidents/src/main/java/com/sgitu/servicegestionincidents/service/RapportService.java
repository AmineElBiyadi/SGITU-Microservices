package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.response.RapportDTO;

import java.util.Map;

public interface RapportService {

    RapportDTO genererRapport(String periode);
    Map<String, Object> genererTableauBord();
}
