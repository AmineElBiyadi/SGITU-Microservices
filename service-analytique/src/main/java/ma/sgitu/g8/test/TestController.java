package ma.sgitu.g8.test;

import lombok.extern.slf4j.Slf4j;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.SnapshotRepository;
import ma.sgitu.g8.scheduler.ScheduledAnalyticsJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ScheduledAnalyticsJob scheduledAnalyticsJob;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @GetMapping("/run")
    public ResponseEntity<String> run() {
        log.info("Manual analytics job execution requested");
        scheduledAnalyticsJob.runAnalytics();
        return ResponseEntity.ok("Job exécuté avec succès");
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<StatSnapshot>> snapshots() {
        return ResponseEntity.ok(
                snapshotRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(0, 100)
                ).getContent()
        );
    }
}
