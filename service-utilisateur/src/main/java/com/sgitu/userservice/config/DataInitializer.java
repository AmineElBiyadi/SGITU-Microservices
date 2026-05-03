package com.sgitu.userservice.config;

import com.sgitu.userservice.entity.Role;
import com.sgitu.userservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initialise les rôles par défaut dans la base de données au démarrage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    private static final List<String> DEFAULT_ROLES = List.of(
            "ROLE_PASSENGER",
            "ROLE_SUBSCRIBER",
            "ROLE_DRIVER",
            "ROLE_OPERATOR",
            "ROLE_TECHNICIAN",
            "ROLE_STAFF",
            "ROLE_ADMIN"
    );

    @Override
    public void run(String... args) {
        for (String roleName : DEFAULT_ROLES) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Rôle créé : {}", roleName);
            }
        }
        log.info("Initialisation des rôles terminée — {} rôles en base", roleRepository.count());
    }
}
