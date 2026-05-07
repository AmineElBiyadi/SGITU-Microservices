package com.sgitu.servicegestionincidents.messaging.config;

import com.sgitu.servicegestionincidents.messaging.constant.MessagingConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // MESSAGE CONVERTER (sends/receives JSON instead of raw bytes)

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // EXCHANGE

    @Bean
    public TopicExchange incidentExchange() {
        return new TopicExchange(MessagingConstants.INCIDENT_EXCHANGE);
    }

    // OUTGOING QUEUES

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(MessagingConstants.NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue transportQueue() {
        return QueueBuilder.durable(MessagingConstants.TRANSPORT_QUEUE).build();
    }

    @Bean
    public Queue analytiqueOutQueue() {
        return QueueBuilder.durable(MessagingConstants.ANALYTIQUE_OUT_QUEUE).build();
    }

    // INCOMING QUEUES

    @Bean
    public Queue suiviVehiculeQueue() {
        return QueueBuilder.durable(MessagingConstants.SUIVI_VEHICULE_QUEUE).build();
    }

    @Bean
    public Queue analytiqueInQueue() {
        return QueueBuilder.durable(MessagingConstants.ANALYTIQUE_IN_QUEUE).build();
    }

    // BINDINGS

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(notificationQueue).to(incidentExchange)
                .with(MessagingConstants.NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding transportBinding(Queue transportQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(transportQueue).to(incidentExchange)
                .with(MessagingConstants.TRANSPORT_ROUTING_KEY);
    }

    @Bean
    public Binding analytiqueOutBinding(Queue analytiqueOutQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(analytiqueOutQueue).to(incidentExchange)
                .with(MessagingConstants.ANALYTIQUE_OUT_ROUTING_KEY);
    }

    @Bean
    public Binding suiviVehiculeBinding(Queue suiviVehiculeQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(suiviVehiculeQueue).to(incidentExchange)
                .with(MessagingConstants.SUIVI_VEHICULE_ROUTING_KEY);
    }

    @Bean
    public Binding analytiqueInBinding(Queue analytiqueInQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(analytiqueInQueue).to(incidentExchange)
                .with(MessagingConstants.ANALYTIQUE_IN_ROUTING_KEY);
    }
}
