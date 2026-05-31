package ma.sgitu.g5.dto.kafka;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AbonnementKafkaEventDTO {
    private String type;
    private String userId;
    private String abonnementId;
    private String timestamp;
    private List<String> channels;
    private Map<String, Object> data;
}
