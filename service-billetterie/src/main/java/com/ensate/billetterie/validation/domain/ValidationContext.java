package com.ensate.billetterie.validation.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationContext {

    //Add other attributes or modify existing ones

    private boolean isDenied;
    private String message;
}
