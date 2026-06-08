package com.sgitu.servicegestionincidents.client;

import com.sgitu.servicegestionincidents.config.FeignConfig;
import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "utilisateur-service", url = "${microservices.utilisateur.url}", configuration = FeignConfig.class)
public interface UtilisateurClient {

    @GetMapping("/api/users/{id}")
    UtilisateurDTO obtenirUtilisateur(@PathVariable Long id);

    @GetMapping("/api/users/roles/{role}")
    java.util.List<UtilisateurDTO> obtenirUtilisateursParRole(@PathVariable String role);
}
