package ma.sgitu.g8.repository;

import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.StatSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatSnapshotRepository extends MongoRepository<StatSnapshot, String> {
    List<StatSnapshot> findBySnapshotType(SnapshotType type);
    List<StatSnapshot> findByPeriod(String period);
    Optional<StatSnapshot> findFirstBySnapshotTypeAndPeriodOrderByComputedAtDesc(SnapshotType type, String period);
}
