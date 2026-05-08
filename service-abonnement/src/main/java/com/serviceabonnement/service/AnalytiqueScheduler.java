package com.serviceabonnement.service;

import com.serviceabonnement.client.AnalyseClient;
import com.serviceabonnement.dto.external.AnalyseEventDTO;
import com.serviceabonnement.entity.AnalytiqueTrace;
import com.serviceabonnement.repository.AnalytiqueTraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalytiqueScheduler {

    private final AnalytiqueTraceRepository repository;
    private final AnalyseClient analyseClient;

    /**
     * Tâche planifiée pour envoyer les traces d'analyse par batch toutes les 30 minutes.
     * Si l'envoi réussit, les traces sont supprimées de la base de données.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Transactional
    public void sendBatchToAnalyse() {
        List<AnalytiqueTrace> traces = repository.findAll();
        
        if (traces.isEmpty()) {
            return;
        }

        log.info("Tentative d'envoi d'un batch de {} traces à l'analyse", traces.size());

        try {
            List<AnalyseEventDTO> dtos = traces.stream()
                    .map(t -> AnalyseEventDTO.builder()
                            .timestamp(t.getTimestamp())
                            .userId(t.getUserId())
                            .action(t.getAction())
                            .planType(t.getPlanType())
                            .build())
                    .collect(Collectors.toList());

            analyseClient.sendEvents(dtos);
            
            // Suppression après succès de l'envoi
            repository.deleteAll(traces);
            log.info("Batch d'analyse envoyé avec succès. {} traces supprimées.", traces.size());
            
        } catch (Exception e) {
            log.error("Échec de l'envoi du batch d'analyse (sera réessayé au prochain cycle): {}", e.getMessage());
        }
    }
}
