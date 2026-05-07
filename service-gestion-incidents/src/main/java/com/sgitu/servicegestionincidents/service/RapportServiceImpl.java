package com.sgitu.servicegestionincidents.service;

import com.sgitu.servicegestionincidents.dto.response.RapportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RapportServiceImpl implements RapportService {

    // TODO

    @Override
    public RapportDTO genererRapport(String periode) {
        // TODO
        return null;
    }

    @Override
    public Map<String, Object> genererTableauBord() {
        // TODO
        return null;
    }
}
