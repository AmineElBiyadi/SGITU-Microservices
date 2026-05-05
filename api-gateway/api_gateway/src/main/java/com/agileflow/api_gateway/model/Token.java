package com.agileflow.api_gateway.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiry;
    private boolean revoked = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}