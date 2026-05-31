package ma.sgitu.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestMobileMoneyResponse {

    private Long id;
    private String maskedPhone;
    private String provider;
    private BigDecimal balance;
    private String status;
}