package com.ensate.billetterie.ticket.domain.enums;

/**
 * Enum representing the lifecycle status of a mission.
 * Used to determine if a ticket can be validated for a mission.
 */
public enum MissionStatus {
    SCHEDULED,   // Mission not yet started
    ONGOING,     // Mission is currently active (accepting entries)
    CLOSED,      // Mission has ended (not accepting new entries)
    CANCELLED    // Mission has been cancelled
}
