package ma.sgitu.g5.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka centralisée pour le service-notification.
 * <p>
 * Inclut un {@link DefaultErrorHandler} avec {@link DeadLetterPublishingRecoverer} :
 * après 3 tentatives infructueuses (2 retry + 1 initiale), le message est
 * automatiquement publié vers le topic "{topic-original}.DLT" pour analyse ou
 * relance manuelle.
 * </p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manuel
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Récupérateur DLT : redirige les messages en échec vers "{topic-original}.DLT".
     * La destination est calculée automatiquement par Spring Kafka.
     *
     * @param kafkaTemplate le template utilisé pour publier vers le DLT
     * @return un {@link DeadLetterPublishingRecoverer} configuré
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<Object, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    /**
     * Gestionnaire d'erreurs avec 3 tentatives au total (1 initiale + 2 retries),
     * sans délai entre les essais (FixedBackOff(0, 2)).
     * En cas d'échec définitif, le message est envoyé au DLT via le recoverer.
     *
     * @param recoverer le {@link DeadLetterPublishingRecoverer} injecté
     * @return un {@link DefaultErrorHandler} configuré
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        // FixedBackOff(interval ms, maxAttempts) — 0 ms entre retries, 2 retry = 3 tentatives au total
        FixedBackOff backOff = new FixedBackOff(0L, 2L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        // Brancher le gestionnaire d'erreurs avec DLT sur la factory
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }
}