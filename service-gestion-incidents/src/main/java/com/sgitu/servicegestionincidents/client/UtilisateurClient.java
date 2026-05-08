package com.sgitu.servicegestionincidents.client;

import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "utilisateur-service", url = "${microservices.utilisateur.url}")
public interface UtilisateurClient {

    @GetMapping("/api/utilisateurs/{id}")
    UtilisateurDTO obtenirUtilisateur(@PathVariable Long id);

    @GetMapping("/api/utilisateurs/{userId}/roles/{role}")
    Boolean verifierRole(@PathVariable Long userId, @PathVariable String role);
}
