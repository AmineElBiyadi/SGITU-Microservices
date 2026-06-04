package com.g7suivivehicules.kafka;

import com.g7suivivehicules.dto.VehiculeRegisteredEvent;
import com.g7suivivehicules.entity.Vehicule;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        // Injection manuelle des valeurs d'annotations @Value
        ReflectionTestUtils.setField(kafkaProducerService, "topicVehicleRegistered", "vehicle.registered");
        ReflectionTestUtils.setField(kafkaProducerService, "topicPositionG4", "vehicule-positions");
    }

    @Test
    void publierVehiculeEnregistre_ShouldCallKafkaTemplate() throws Exception {
        // Arrange
        UUID vehiculeId = UUID.randomUUID();
        VehiculeRegisteredEvent event = VehiculeRegisteredEvent.builder()
                .vehiculeId(vehiculeId)
                .immatriculation("BUS-G7-123")
                .type(Vehicule.TypeVehicule.BUS)
                .ligne("G4")
                .build();

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("vehicle.registered", 0), 0, 0, 0, 0, 0
        );
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("vehicle.registered"), eq(vehiculeId.toString()), eq(event)))
                .thenReturn(future);

        // Act
        kafkaProducerService.publierVehiculeEnregistre(event);

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("vehicle.registered"), eq(vehiculeId.toString()), eq(event));
    }
}
