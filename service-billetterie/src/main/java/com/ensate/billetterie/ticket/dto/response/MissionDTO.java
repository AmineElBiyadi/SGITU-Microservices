package com.ensate.billetterie.ticket.dto.response;

import com.ensate.billetterie.ticket.domain.enums.MissionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO representing a mission received from the Coordination microservice.
 *
 * This is NOT a MongoDB entity - it's only a data transfer object.
 * The actual mission data is stored and managed by the Coordination microservice.
 * Billetterie only holds a reference (tripId) and retrieves mission data on-demand.
 *
 * This follows the microservices architecture principle:
 * Each microservice owns its data, other services only read it via APIs.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MissionDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private MissionStatus status;

    @JsonProperty("starts_at")
    private Instant startsAt;

    @JsonProperty("ends_at")
    private Instant endsAt;

    @JsonProperty("entry_opens_at")
    private Instant entryOpensAt;

    @JsonProperty("entry_closes_at")
    private Instant entryClosesAt;

    /**
     * Check if the mission is currently accepting ticket validations.
     * Returns true if:
     * - Status is ONGOING
     * - Current time is between entryOpensAt and entryClosesAt
     *
     * @return true if mission is open for entry, false otherwise
     */
    public boolean isOpenForEntry() {
        // Status must be ONGOING
        if (status != MissionStatus.ONGOING) {
            return false;
        }

        Instant now = Instant.now();

        // Check if entry window has opened
        if (entryOpensAt != null && now.isBefore(entryOpensAt)) {
            return false;
        }

        // Check if entry window has closed
        if (entryClosesAt != null && now.isAfter(entryClosesAt)) {
            return false;
        }

        return true;
    }
}
