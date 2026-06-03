package com.serviceabonnement;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Désactivé pour GitHub Actions car il n'a pas accès à la base de données")
class ServiceAbonnementApplicationTests {

    @Test
    void contextLoads() {
    }

}
