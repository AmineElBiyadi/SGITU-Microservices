package ma.sgitu.g5.dto.kafka;

import lombok.Data;

@Data
public class UserKafkaEventDTO {
    private String eventType;
    private String userId;
    private String email;
    private String username;
    private String timestamp;
}
