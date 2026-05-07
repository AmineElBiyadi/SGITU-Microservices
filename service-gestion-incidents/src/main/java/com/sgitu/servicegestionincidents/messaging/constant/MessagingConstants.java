package com.sgitu.servicegestionincidents.messaging.constant;

public final class MessagingConstants {

    private MessagingConstants() {}

    // ===== EXCHANGES =====
    public static final String INCIDENT_EXCHANGE = "incident.exchange";

    // ===== QUEUES =====
    // Outgoing: we publish, other services consume
    public static final String NOTIFICATION_QUEUE = "incident.notification.queue";
    public static final String TRANSPORT_QUEUE = "incident.transport.queue";
    public static final String ANALYTIQUE_OUT_QUEUE = "incident.analytique.queue";

    // Incoming: other services publish, we consume
    public static final String SUIVI_VEHICULE_QUEUE = "suivi-vehicule.incident.queue";
    public static final String ANALYTIQUE_IN_QUEUE = "analytique.incident.queue";

    // ===== ROUTING KEYS =====
    public static final String NOTIFICATION_ROUTING_KEY = "incident.notification";
    public static final String TRANSPORT_ROUTING_KEY = "incident.transport";
    public static final String ANALYTIQUE_OUT_ROUTING_KEY = "incident.analytique";
    public static final String SUIVI_VEHICULE_ROUTING_KEY = "suivi-vehicule.incident.detected";
    public static final String ANALYTIQUE_IN_ROUTING_KEY = "analytique.incident.detected";
}
