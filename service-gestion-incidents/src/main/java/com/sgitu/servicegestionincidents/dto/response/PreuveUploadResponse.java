package com.sgitu.servicegestionincidents.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PreuveUploadResponse {
    private String uploadUrl;     // URL pré-signée à appeler en PUT par le client
    private String stockageKey;   // Clé unique générée par le serveur à conserver pour l'enregistrement
}
