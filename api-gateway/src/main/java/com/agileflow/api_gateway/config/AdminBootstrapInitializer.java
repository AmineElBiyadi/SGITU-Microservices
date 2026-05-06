package com.agileflow.api_gateway.config;

import com.agileflow.api_gateway.model.User;
import com.agileflow.api_gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${g10.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${g10.admin.email:admin@sgitu.ma}")
    private String adminEmail;

    @Value("${g10.admin.password:Admin123456}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        userRepository.findByEmail(adminEmail)
                .ifPresentOrElse(this::ensureAdminAccount, this::createAdminAccount);
    }

    private void createAdminAccount() {
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(User.RoleType.ROLE_ADMIN);
        admin.setEnabled(true);
        admin.setEmailVerified(true);

        userRepository.save(admin);
        log.info("Compte admin initial G10 cree: {}", adminEmail);
    }

    private void ensureAdminAccount(User admin) {
        boolean changed = false;

        if (admin.getRole() != User.RoleType.ROLE_ADMIN) {
            admin.setRole(User.RoleType.ROLE_ADMIN);
            changed = true;
        }
        if (!admin.isEnabled()) {
            admin.setEnabled(true);
            changed = true;
        }
        if (!admin.isEmailVerified()) {
            admin.setEmailVerified(true);
            changed = true;
        }

        if (changed) {
            userRepository.save(admin);
            log.info("Compte admin initial G10 synchronise: {}", adminEmail);
        }
    }
}
