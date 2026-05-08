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
            "ROLE_PASSENGER",   // G1, G2 — regular customer
            "ROLE_STUDENT",     // G2 — student identity (discount subscriptions)
            "ROLE_DRIVER",      // G4 — bus/tram driver
            "ROLE_DISPATCHER",
            "ROLE_OPERATOR",      // G4, G9 — operational staff
            "ROLE_OPERATOR_G4",    // G7 — IoT/tracking operator
            "ROLE_TECHNICIAN",  // G9 — incident technician
            "ROLE_ADMIN",
            "ROLE_ADMIN_G1",
            "ROLE_ADMIN_G2",
            "ROLE_ADMIN_G4",
            "ROLE_ADMIN_G7",
            "ROLE_ADMIN_G9"
                   // G10 — system administrator
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
