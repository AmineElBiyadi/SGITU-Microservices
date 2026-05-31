package com.sgitu.servicegestionincidents.messaging.constant;

public final class MessagingConstants {

    private MessagingConstants() {}

    // GROUP ID
    public static final String GROUP_ID = "gestion-incidents-group";

    // TOPICS
    // Outgoing
    public static final String NOTIFICATION_TOPIC = "incident.notification.topic";
    public static final String TRANSPORT_TOPIC = "incident.transport.topic";
    public static final String ANALYTIQUE_OUT_TOPIC = "incident.analytique.topic";

    // Incoming
    public static final String SUIVI_VEHICULE_TOPIC = "suivi-vehicule.incident.topic";
}
