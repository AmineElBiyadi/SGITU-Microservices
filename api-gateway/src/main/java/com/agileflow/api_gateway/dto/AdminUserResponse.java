package com.agileflow.api_gateway.dto;

import com.agileflow.api_gateway.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String email;
    private String role;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
