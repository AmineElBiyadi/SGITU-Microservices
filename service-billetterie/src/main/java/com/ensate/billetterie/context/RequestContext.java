package com.ensate.billetterie.context;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RequestContext {

    private final UUID userId;
    private final String userEmail;
    private final List<String> roles;
    private final String correlationId;

    public boolean hasRole(String role) {
        if (roles == null) return false;
        String normalized = role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
        return roles.contains(normalized);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
