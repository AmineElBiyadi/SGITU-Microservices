package com.sgitu.servicegestionincidents.service.utilisateur;

import com.sgitu.servicegestionincidents.client.UtilisateurClient;
import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurClient utilisateurClient;

    @Override
    @CircuitBreaker(name = "utilisateurService", fallbackMethod = "findByIdFallback")
    @Retry(name = "utilisateurService")
    @TimeLimiter(name = "utilisateurService")
    public CompletableFuture<UtilisateurDTO> findById(Long id) {
        try {
            return CompletableFuture.completedFuture(utilisateurClient.obtenirUtilisateur(id));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @CircuitBreaker(name = "utilisateurService", fallbackMethod = "findByRoleFallback")
    @Retry(name = "utilisateurService")
    @TimeLimiter(name = "utilisateurService")
    public CompletableFuture<List<UtilisateurDTO>> findByRole(String role) {
        try {
            return CompletableFuture.completedFuture(utilisateurClient.obtenirUtilisateursParRole(role));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("unused")
    private CompletableFuture<UtilisateurDTO> findByIdFallback(Long id, Throwable t) {
        log.warn("G3 indisponible pour userId {} : {}", id, t.getMessage());
        return CompletableFuture.completedFuture(null);
    }

    @SuppressWarnings("unused")
    private CompletableFuture<List<UtilisateurDTO>> findByRoleFallback(String role, Throwable t) {
        log.warn("G3 indisponible pour le rôle {} : {}", role, t.getMessage());
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
