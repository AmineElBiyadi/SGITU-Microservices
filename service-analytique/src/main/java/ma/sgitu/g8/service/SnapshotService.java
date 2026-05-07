package ma.sgitu.g8.service;

import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.SnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SnapshotService {

    private final SnapshotRepository snapshotRepository;

    public SnapshotService(SnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    public StatSnapshot upsert(String statId, SnapshotType type, Object value) {
        StatSnapshot existing = snapshotRepository.findByStatId(statId).orElse(null);
        StatSnapshot incoming = null;

        if (value instanceof StatSnapshot) {
            incoming = (StatSnapshot) value;
        }

        if (existing != null) {
            if (incoming != null) {
                existing.setValue(incoming.getValue());
                existing.setPeriod(incoming.getPeriod());
                existing.setGranularity(incoming.getGranularity());
                existing.setMetadata(incoming.getMetadata());
                existing.setPrediction(incoming.isPrediction());
            } else if (value instanceof Number) {
                existing.setValue(((Number) value).doubleValue());
            }
            existing.setComputedAt(LocalDateTime.now());
            return snapshotRepository.save(existing);
        } else {
            StatSnapshot newSnap = incoming != null ? incoming : new StatSnapshot();
            newSnap.setStatId(statId);
            newSnap.setSnapshotType(type);
            if (incoming == null && value instanceof Number) {
                newSnap.setValue(((Number) value).doubleValue());
            }
            newSnap.setComputedAt(LocalDateTime.now());
            return snapshotRepository.save(newSnap);
        }
    }
}
