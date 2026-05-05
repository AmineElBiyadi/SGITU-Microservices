package com.agileflow.api_gateway.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {

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

    // ────────────────────────────────────────────────
    //  UserDetails — Spring Security
    // ────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;  // IMPORTANT : doit retourner le mot de passe encodé
    }

    @Override
    public String getUsername() {
        return email;     // Spring Security utilise l'email comme username
    }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled()               { return enabled; }

    // ────────────────────────────────────────────────
    //  Setters explicites (nécessaires car Lombok
    //  ne génère pas setPassword quand getPassword
    //  est surchargé depuis UserDetails)
    // ────────────────────────────────────────────────

    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(RoleType role)       { this.role = role; }
    public void setEnabled(boolean enabled)  { this.enabled = enabled; }

    // ────────────────────────────────────────────────
    //  Enum des rôles
    // ────────────────────────────────────────────────

    public enum RoleType {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_DRIVER,
        ROLE_PASSENGER,
        ROLE_STUDENT,
        ROLE_OPERATOR,
        ROLE_TECHNICIAN,
        ROLE_STAFF
    }
}