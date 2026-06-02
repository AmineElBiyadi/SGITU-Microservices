package com.sgitu.servicegestionincidents.service.utilisateur;

import com.sgitu.servicegestionincidents.client.UtilisateurClient;
import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class UtilisateurServiceImplTest {

    @MockitoBean
    private UtilisateurClient utilisateurClient;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("utilisateurService").reset();
    }

    @Test
    void findById_returnsNullWhenClientFails() {
        when(utilisateurClient.obtenirUtilisateur(1L)).thenThrow(new RuntimeException("G3 down"));

        UtilisateurDTO result = utilisateurService.findById(1L).join();

        assertThat(result).isNull();
        verify(utilisateurClient, times(1)).obtenirUtilisateur(1L);
    }

    @Test
    void findByRole_returnsEmptyListWhenClientFails() {
        when(utilisateurClient.obtenirUtilisateursParRole("ROLE_DISPATCHER"))
                .thenThrow(new RuntimeException("G3 down"));

        List<UtilisateurDTO> result = utilisateurService.findByRole("ROLE_DISPATCHER").join();

        assertThat(result).isEmpty();
        verify(utilisateurClient, times(1)).obtenirUtilisateursParRole("ROLE_DISPATCHER");
    }

    @Test
    void findById_returnsUserWhenClientSucceeds() {
        UtilisateurDTO user = UtilisateurDTO.builder()
                .id(1L)
                .email("user@sgitu.ma")
                .build();
        when(utilisateurClient.obtenirUtilisateur(1L)).thenReturn(user);

        UtilisateurDTO result = utilisateurService.findById(1L).join();

        assertThat(result).isEqualTo(user);
    }
}
