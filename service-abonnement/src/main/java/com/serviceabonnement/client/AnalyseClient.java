package com.serviceabonnement.client;

import com.serviceabonnement.dto.external.AnalyseEventDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "service-analyse", url = "${application.config.analyse-service-url}")
public interface AnalyseClient {

    @PostMapping("/api/events/batch")
    void sendEvents(@RequestBody List<AnalyseEventDTO> events);
}
