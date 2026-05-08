package com.sgitu.userservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Identifiants de connexion")
public class LoginRequestDTO {
    @NotBlank(message = "L email est obligatoire")
    @Email(message = "Format d email invalide")
    @Schema(description = "Adresse email", example = "user@sgitu.ma")
    private String email;
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Schema(description = "Mot de passe", example = "P@ssw0rd!")
    private String password;
}