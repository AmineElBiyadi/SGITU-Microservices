package com.ensate.billetterie.ticket.dto.result;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    private boolean isDenied;
    private String message;
}
