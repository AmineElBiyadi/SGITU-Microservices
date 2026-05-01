package com.ensate.billetterie.validation.interfaces;

import com.ensate.billetterie.validation.domain.ValidationContext;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface NextStep {
    CompletableFuture<ValidationContext> execute(ValidationContext context);
}
