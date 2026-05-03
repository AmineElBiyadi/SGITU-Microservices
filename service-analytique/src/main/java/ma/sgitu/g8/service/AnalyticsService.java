package ma.sgitu.g8.service;

import ma.sgitu.g8.model.Report;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.ReportRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final StatSnapshotRepository snapshotRepository;
    private final ReportRepository reportRepository;

    public AnalyticsService(StatSnapshotRepository snapshotRepository, ReportRepository reportRepository) {
        this.snapshotRepository = snapshotRepository;
        this.reportRepository = reportRepository;
    }

    public Optional<StatSnapshot> getSnapshotByTypeAndPeriod(SnapshotType type, String period) {
        return snapshotRepository.findBySnapshotTypeAndPeriod(type, period);
    }
    
    public List<StatSnapshot> getAllSnapshots() {
        return snapshotRepository.findAll();
    }

    public List<StatSnapshot> getSnapshotsByType(SnapshotType type) {
        return snapshotRepository.findBySnapshotType(type);
    }

    public Report generateReport(String period, List<SnapshotType> types) {
        List<StatSnapshot> snapshots = types.stream()
                .map(type -> snapshotRepository.findBySnapshotTypeAndPeriod(type, period))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Report report = Report.builder()
                .generatedAt(LocalDateTime.now())
                .period(period)
                .requestedTypes(types)
                .snapshots(snapshots)
                .build();

        return reportRepository.save(report);
    }

    public Optional<Report> getReportById(String id) {
        return reportRepository.findById(id);
    }
}
