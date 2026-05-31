package com.sgitu.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event payload sent to external consumers (e.g. analytics group)
 * when a user's active/inactive status changes.
 *
 * Format agreed with consuming group:
 * {
 *   "userId": "42",
 *   "action": "active" | "inactive",
 *   "timestamp": "2026-05-01T08:45:00Z"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusEventDTO {

    /** User ID as a string (numeric, matching the Long PK of the users table). */
    private String userId;

    /** "active" when a user is created or reactivated, "inactive" when deactivated. */
    private String action;

    /** ISO-8601 UTC timestamp of when the state change occurred. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;
}

