package ma.sgitu.g5.integration;

import ma.sgitu.g5.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"user-events", "payment.notification", "notifications", "ticket.created"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=true")
@DirtiesContext
@DisplayName("Tests d'intégration — Consommation Kafka inter-microservices")
class KafkaMicroserviceIntegrationTest extends AbstractG5IntegrationTest {

    @Autowired private NotificationRepository notificationRepository;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp(@Autowired org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafka) {
        notificationRepository.deleteAll();
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    @DisplayName("G3 user-events → notification persistée G3_UTILISATEUR")
    void g3_userEvents_createsNotification() {
        String payload = """
                {
                  "eventType": "WELCOME",
                  "userId": "kafka-user-001",
                  "email": "kafka@sgitu.ma",
                  "username": "KafkaUser"
                }
                """;

        kafkaTemplate.send("user-events", "kafka-user-001", payload);

        waitUntil(() -> notificationRepository.findAll().stream()
                .anyMatch(n -> "G3_UTILISATEUR".equals(n.getSourceService())
                        && "kafka-user-001".equals(n.getUserId())), 15_000);
    }

    @Test
    @DisplayName("G1 ticket.created → 2 notifications (EMAIL + PUSH)")
    void g1_ticketCreated_dualChannel() {
        String payload = """
                {
                  "eventType": "TICKET_ISSUED",
                  "userId": "ticket-user-1",
                  "ticketId": "TK-777",
                  "email": "ticket@sgitu.ma",
                  "deviceToken": "fcm-test-token"
                }
                """;

        kafkaTemplate.send("ticket.created", "TK-777", payload);

        waitUntil(() -> notificationRepository.findAll().stream()
                .filter(n -> "G1_BILLETTERIE".equals(n.getSourceService()))
                .count() >= 2, 20_000);
    }

    @Test
    @DisplayName("G6 payment.notification → notification G6_PAYMENT")
    void g6_paymentNotification() {
        String payload = """
                {
                  "eventType": "PAYMENT_SUCCESS",
                  "channel": "EMAIL",
                  "userId": "pay-user-1",
                  "recipient": {
                    "userId": "pay-user-1",
                    "email": "pay@sgitu.ma"
                  },
                  "metadata": {
                    "amount": "99",
                    "invoiceNumber": "INV-KAFKA-1"
                  }
                }
                """;

        kafkaTemplate.send("payment.notification", "pay-user-1", payload);

        waitUntil(() -> notificationRepository.findAll().stream()
                .anyMatch(n -> "G6_PAYMENT".equals(n.getSourceService())), 15_000);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(condition.getAsBoolean())
                .as("Condition non satisfaite après %d ms — notifications=%s", timeoutMs, notificationRepository.findAll())
                .isTrue();
    }
}
