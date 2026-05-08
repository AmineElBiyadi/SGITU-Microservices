package com.sgitu.userservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reponse d authentification contenant le token JWT")
public class LoginResponseDTO {
    @Schema(description = "JWT signe a inclure dans Authorization: Bearer <token>")
    private String token;
    @Schema(description = "ID de l utilisateur authentifie")
    private Long userId;
    @Schema(description = "Email de l utilisateur authentifie")
    private String email;
    @Schema(description = "Roles de l utilisateur")
    private List<String> roles;
}