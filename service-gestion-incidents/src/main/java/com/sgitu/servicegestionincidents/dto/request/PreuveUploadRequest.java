package com.sgitu.servicegestionincidents.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PreuveUploadRequest {
    @NotBlank(message = "Le nom du fichier est obligatoire")
    private String nomFichier;

    @NotBlank(message = "Le type MIME (content-type) est obligatoire")
    private String contentType;
    
    private String description;
}
