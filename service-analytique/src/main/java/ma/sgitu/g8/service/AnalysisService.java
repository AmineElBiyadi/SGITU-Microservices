package ma.sgitu.g8.service;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AnalysisService {

    private final Map<String, AnalysisDetailResponse> analyses = new ConcurrentHashMap<>();
    private final Map<String, AnalysisRequest.AnalysisType> analysisTypes = new ConcurrentHashMap<>();

    public AnalysisResponse launchAnalysis(AnalysisRequest request) {
        String analysisId = UUID.randomUUID().toString();
        
        AnalysisResponse response = new AnalysisResponse();
        response.setAnalysisId(analysisId);
        response.setStatus("PENDING");
        response.setCreatedAt(LocalDateTime.now());
        response.setType(request.getType().getValue());
        response.setDatasetId(request.getDatasetId());
        
        AnalysisDetailResponse detail = new AnalysisDetailResponse();
        detail.setAnalysisId(analysisId);
        detail.setStatus("PENDING");
        detail.setStartedAt(LocalDateTime.now());
        detail.setType(request.getType().getValue());
        detail.setDatasetId(request.getDatasetId());
        
        analyses.put(analysisId, detail);
        analysisTypes.put(analysisId, request.getType());
        
        log.info("Analysis {} launched for dataset: {}", analysisId, request.getDatasetId());
        
        return response;
    }

    public Optional<AnalysisDetailResponse> getAnalysisById(String analysisId) {
        return Optional.ofNullable(analyses.get(analysisId));
    }

    public Page<AnalysisListResponse.AnalysisSummary> listAnalyses(Pageable pageable, String status) {
        List<AnalysisListResponse.AnalysisSummary> allAnalyses = analyses.values().stream()
                .filter(analysis -> status == null || status.equals(analysis.getStatus()))
                .map(this::convertToSummary)
                .toList();
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allAnalyses.size());
        List<AnalysisListResponse.AnalysisSummary> pageContent = allAnalyses.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allAnalyses.size());
    }

    public boolean deleteAnalysis(String analysisId) {
        AnalysisDetailResponse analysis = analyses.get(analysisId);
        if (analysis == null) {
            return false;
        }
        
        if ("RUNNING".equals(analysis.getStatus())) {
            throw new IllegalStateException("Cannot delete running analysis");
        }
        
        analyses.remove(analysisId);
        analysisTypes.remove(analysisId);
        
        log.info("Analysis {} deleted", analysisId);
        return true;
    }

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        long totalAnalyses = analyses.size();
        long completedAnalyses = analyses.values().stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .count();
        long failedAnalyses = analyses.values().stream()
                .filter(a -> "FAILED".equals(a.getStatus()))
                .count();
        long runningAnalyses = analyses.values().stream()
                .filter(a -> "RUNNING".equals(a.getStatus()))
                .count();
        long pendingAnalyses = analyses.values().stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .count();
        
        stats.setTotalAnalyses(totalAnalyses);
        stats.setCompletedAnalyses(completedAnalyses);
        stats.setFailedAnalyses(failedAnalyses);
        stats.setRunningAnalyses(runningAnalyses);
        stats.setPendingAnalyses(pendingAnalyses);
        
        double successRate = totalAnalyses > 0 ? (completedAnalyses * 100.0 / totalAnalyses) : 0.0;
        stats.setSuccessRate(successRate);
        
        stats.setAverageExecutionTimeMs(250000.0);
        
        DashboardStats.AnalysisTypeStats typeStats = new DashboardStats.AnalysisTypeStats();
        typeStats.setStatistique(analysisTypes.values().stream()
                .filter(t -> t == AnalysisRequest.AnalysisType.STATISTIQUE)
                .count());
        typeStats.setRegression(analysisTypes.values().stream()
                .filter(t -> t == AnalysisRequest.AnalysisType.REGRESSION)
                .count());
        typeStats.setClassification(analysisTypes.values().stream()
                .filter(t -> t == AnalysisRequest.AnalysisType.CLASSIFICATION)
                .count());
        stats.setAnalysesByType(typeStats);
        
        return stats;
    }

    private AnalysisListResponse.AnalysisSummary convertToSummary(AnalysisDetailResponse analysis) {
        AnalysisListResponse.AnalysisSummary summary = new AnalysisListResponse.AnalysisSummary();
        summary.setAnalysisId(analysis.getAnalysisId());
        summary.setStatus(analysis.getStatus());
        summary.setType(analysis.getType());
        summary.setDatasetId(analysis.getDatasetId());
        summary.setCreatedAt(analysis.getStartedAt().toString());
        return summary;
    }
}
