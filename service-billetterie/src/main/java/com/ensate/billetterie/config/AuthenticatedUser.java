package com.ensate.billetterie.config;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.UUID;

@Getter
public class AuthenticatedUser extends User {

    private final UUID id;

    public AuthenticatedUser(UUID id, String email, List<SimpleGrantedAuthority> authorities) {
        super(email, "", authorities);
        this.id = id;
    }

    public String getEmail() {
        return getUsername();
    }
}
