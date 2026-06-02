package ma.sgitu.g5.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.sgitu.g5.dto.request.NotificationRequestDTO;
import ma.sgitu.g5.dto.response.NotificationResponseDTO;
import ma.sgitu.g5.service.INotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires — KafkaConsumerController (intégration Kafka simulée)")
class KafkaConsumerControllerTest {

    @Mock private INotificationService notificationService;

    private KafkaConsumerController kafkaConsumerController;

    private ObjectMapper objectMapper;
    private Acknowledgment acknowledgment;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        kafkaConsumerController = new KafkaConsumerController(notificationService, objectMapper);
        acknowledgment = mock(Acknowledgment.class);
        when(notificationService.send(any())).thenReturn(new NotificationResponseDTO());
    }

    @Test
    @DisplayName("G1 ticket.created → EMAIL + PUSH (2 notifications)")
    void handleTicketEvent_g1() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "TICKET_ISSUED",
                "userId", "u-100",
                "ticketId", "T-55",
                "email", "client@sgitu.ma",
                "deviceToken", "fcm-abc"
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "ticket.created", 0, 0L, "key", objectMapper.writeValueAsString(event));

        kafkaConsumerController.handleTicketEvent(record, acknowledgment);

        ArgumentCaptor<NotificationRequestDTO> captor = ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService, times(2)).send(captor.capture());
        verify(acknowledgment).acknowledge();

        List<NotificationRequestDTO> sent = captor.getAllValues();
        assertThat(sent).extracting(NotificationRequestDTO::getChannel).containsExactly("EMAIL", "PUSH");
        assertThat(sent).allMatch(d -> "G1_BILLETTERIE".equals(d.getSourceService()));
    }

    @Test
    @DisplayName("G2 abonnement.renouvellement → canaux du payload")
    void handleAbonnementEvent_g2() throws Exception {
        Map<String, Object> event = Map.of(
                "type", "RENOUVELLEMENT_EFFECTUE",
                "userId", "u-200",
                "abonnementId", "AB-9",
                "channels", List.of("EMAIL", "SMS"),
                "data", Map.of("planNom", "Mensuel"),
                "email", "abonne@sgitu.ma",
                "phone", "+212611111111"
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "abonnement.renouvellement", 0, 1L, "key", objectMapper.writeValueAsString(event));

        kafkaConsumerController.handleAbonnementEvent(record, acknowledgment);

        verify(notificationService, times(2)).send(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("G3 user-events → EMAIL unique")
    void handleUserEvent_g3() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "WELCOME",
                "userId", "u-300",
                "email", "new@sgitu.ma",
                "username", "Ahmed"
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "user-events", 0, 2L, "key", objectMapper.writeValueAsString(event));

        kafkaConsumerController.handleUserEvent(record, acknowledgment);

        ArgumentCaptor<NotificationRequestDTO> captor = ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getSourceService()).isEqualTo("G3_UTILISATEUR");
        assertThat(captor.getValue().getChannel()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("G6 payment.notification → canal du payload")
    void handlePaymentNotification_g6() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "PAYMENT_SUCCESS",
                "channel", "EMAIL",
                "userId", "u-600",
                "recipient", Map.of("email", "pay@sgitu.ma", "userId", "u-600"),
                "metadata", Map.of("amount", "150", "invoiceNumber", "INV-1")
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment.notification", 0, 3L, "key", objectMapper.writeValueAsString(event));

        kafkaConsumerController.handlePaymentNotification(record, acknowledgment);

        ArgumentCaptor<NotificationRequestDTO> captor = ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getSourceService()).isEqualTo("G6_PAYMENT");
    }

    @Test
    @DisplayName("G9 notifications → multi-canaux")
    void handleG9Notification() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "INCIDENT_ALERT",
                "channels", List.of("EMAIL", "SMS"),
                "recipient", Map.of("userId", "agent-1", "email", "agent@sgitu.ma", "phone", "+212622222222"),
                "metadata", Map.of("reference", "INC-99", "localisation", "Casablanca")
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "notifications", 0, 4L, "key", objectMapper.writeValueAsString(event));

        kafkaConsumerController.handleG9Notification(record, acknowledgment);

        verify(notificationService, times(2)).send(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("DLT → relance EMAIL avec id déterministe")
    void handleDeadLetterEvent() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "TICKET_ISSUED",
                "userId", "u-dlt",
                "email", "dlt@sgitu.ma"
        );
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "ticket.created.DLT", 0, 5L, "key", objectMapper.writeValueAsString(event));
        record.headers().add(new RecordHeader(
                KafkaHeaders.DLT_ORIGINAL_TOPIC,
                "ticket.created".getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(
                KafkaHeaders.DLT_ORIGINAL_OFFSET,
                "42".getBytes(StandardCharsets.UTF_8)));

        kafkaConsumerController.handleDeadLetterEvent(record, acknowledgment);

        ArgumentCaptor<NotificationRequestDTO> captor = ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(notificationService).send(captor.capture());
        assertThat(captor.getValue().getNotificationId()).contains("DLT-ticket.created-42");
        assertThat(captor.getValue().getSourceService()).isEqualTo("DLT_RETRY");
    }
}
