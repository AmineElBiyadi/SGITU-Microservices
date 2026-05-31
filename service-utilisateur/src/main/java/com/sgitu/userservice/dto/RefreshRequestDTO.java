package com.sgitu.userservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requete de rafraichissement de token")
public class RefreshRequestDTO {
    @NotBlank(message = "Le refresh token est obligatoire")
    @Schema(description = "Le refresh token obtenu lors du login", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;
}
