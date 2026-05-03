package ma.sgitu.g8.repository;

import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.StatSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SnapshotRepository extends MongoRepository<StatSnapshot, String> {

    @Query(value = "{ 'snapshotType': ?0 }", sort = "{ 'computedAt': -1 }")
    Optional<StatSnapshot> findByType(SnapshotType type);

    Optional<StatSnapshot> findFirstByStatIdOrderByComputedAtDesc(String statId);

    List<StatSnapshot> findTop30ByStatIdOrderByComputedAtDesc(String statId);
}
