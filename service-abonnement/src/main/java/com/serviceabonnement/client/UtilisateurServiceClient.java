package com.serviceabonnement.client;

import com.serviceabonnement.dto.external.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@FeignClient(name = "service-utilisateur", url = "${application.config.utilisateur-service-url}")
public interface UtilisateurServiceClient {

    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
