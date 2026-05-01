package com.ensate.billetterie.validation.interfaces;

import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;

import java.util.concurrent.CompletableFuture;

public interface ValidationStep {
    String getStepName();

    CompletableFuture<ValidationContext> execute(
            ValidationContext context,
            NextStep next
    );
}
