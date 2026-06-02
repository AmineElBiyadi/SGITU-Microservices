package com.sgitu.servicegestionincidents.service.utilisateur;

import com.sgitu.servicegestionincidents.dto.response.UtilisateurDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UtilisateurService {

    CompletableFuture<UtilisateurDTO> findById(Long id);

    CompletableFuture<List<UtilisateurDTO>> findByRole(String role);
}
