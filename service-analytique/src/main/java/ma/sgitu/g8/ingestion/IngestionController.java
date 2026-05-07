package ma.sgitu.g8.ingestion;

import com.mongodb.MongoException;
import jakarta.validation.constraints.NotEmpty;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.SourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/tickets")
    public ResponseEntity<BatchIngestionResponse> ingestTickets(
            @RequestBody @NotEmpty List<Map<String, Object>> rawEvents) {
        return ingestBySource(rawEvents, SourceType.TICKETING);
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<BatchIngestionResponse> ingestSubscriptions(
            @RequestBody @NotEmpty List<Map<String, Object>> rawEvents) {
        return ingestBySource(rawEvents, SourceType.SUBSCRIPTION);
    }

    @PostMapping("/payments")
    public ResponseEntity<BatchIngestionResponse> ingestPayments(
            @RequestBody @NotEmpty List<Map<String, Object>> rawEvents) {
        return ingestBySource(rawEvents, SourceType.PAYMENT);
    }

    @PostMapping("/vehicles")
    public ResponseEntity<BatchIngestionResponse> ingestVehicles(
            @RequestBody @NotEmpty List<Map<String, Object>> rawEvents) {
        return ingestBySource(rawEvents, SourceType.VEHICLE);
    }

    @PostMapping("/incidents")
    public ResponseEntity<BatchIngestionResponse> ingestIncidents(
            @RequestBody @NotEmpty List<Map<String, Object>> rawEvents) {
        return ingestBySource(rawEvents, SourceType.INCIDENT);
    }

    @PostMapping("/users")
    public ResponseEntity<BatchIngestionResponse> ingestUsers(
            @RequestBody @NotEmpty List<Map<String, Object>> rawEvents) {
        return ingestBySource(rawEvents, SourceType.USER);
    }

    private ResponseEntity<BatchIngestionResponse> ingestBySource(
            List<Map<String, Object>> rawEvents,
            SourceType sourceType
    ) {
        try {
            BatchIngestionResponse response = ingestionService.ingest(rawEvents, sourceType);
            return ResponseEntity.status(resolveStatus(response)).body(response);
        } catch (DataAccessException | MongoException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private HttpStatus resolveStatus(BatchIngestionResponse response) {
        return switch (response.getStatus()) {
            case "SUCCESS" -> HttpStatus.CREATED;
            case "PARTIAL" -> HttpStatus.MULTI_STATUS;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
