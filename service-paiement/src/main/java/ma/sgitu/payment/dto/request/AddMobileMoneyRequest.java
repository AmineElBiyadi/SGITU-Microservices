package ma.sgitu.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class AddMobileMoneyRequest {
    private Long userId;
    private String phoneNumber;
    private String provider; // INWI, ORANGE, IAM
    private String email; // Pour envoyer l'OTP via G5
}