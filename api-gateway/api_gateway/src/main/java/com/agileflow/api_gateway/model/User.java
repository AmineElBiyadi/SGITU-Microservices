package com.agileflow.api_gateway.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    private boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RoleType {
        ROLE_USER, ROLE_ADMIN, ROLE_DRIVER
    }
}