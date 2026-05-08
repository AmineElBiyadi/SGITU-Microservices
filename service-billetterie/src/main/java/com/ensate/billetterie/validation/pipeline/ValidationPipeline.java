package com.ensate.billetterie.validation.pipeline;

import com.ensate.billetterie.ticket.dto.result.ValidationResult;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.exceptions.ValidationException;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Validation pipeline that executes a series of validation steps in sequence.
 * Uses the Chain of Responsibility pattern with CompletableFuture for async execution.
 * 
 * If any step throws ValidationException, the pipeline stops immediately and
 * the exception is converted to a ValidationResult with failure details.
 * 
 * If all steps succeed, a ValidationResult with valid=true is returned.
 */
public class ValidationPipeline {

    private final List<ValidationStep> steps = new ArrayList<>();

    /**
     * Add a validation step to the pipeline.
     * Steps are executed in the order they are added.
     */
    public ValidationPipeline addStep(ValidationStep step) {
        steps.add(step);
        return this;
    }

    /**
     * Execute the pipeline on the given context.
     * Returns a CompletableFuture containing the validation result.
     */
    public CompletableFuture<ValidationResult> execute(ValidationContext context) {
        NextStep runner = buildChain(0);

        return runner.execute(context)
                .thenApply(ctx -> ValidationResult.success())
                .exceptionally(this::handleException);
    }

    /**
     * Build a chain of steps to be executed sequentially.
     */
    private NextStep buildChain(int index) {
        if (index >= steps.size()) {
            // End of chain - return the context as-is
            return CompletableFuture::completedFuture;
        }

        return ctx -> {
            ValidationStep currentStep = steps.get(index);
            return currentStep.execute(ctx, buildChain(index + 1));
        };
    }

    /**
     * Handle exceptions from the pipeline execution.
     * If the exception is a ValidationException, convert it to a ValidationResult.
     */
    private ValidationResult handleException(Throwable throwable) {
        if (throwable instanceof CompletionException) {
            Throwable cause = throwable.getCause();
            if (cause instanceof ValidationException) {
                ValidationException validationEx = (ValidationException) cause;
                return ValidationResult.failure(
                        validationEx.getFailedStep(),
                        validationEx.getReason()
                );
            }
        } else if (throwable instanceof ValidationException) {
            ValidationException validationEx = (ValidationException) throwable;
            return ValidationResult.failure(
                    validationEx.getFailedStep(),
                    validationEx.getReason()
            );
        }
        
        // Unexpected exception
        return ValidationResult.failure(
                "UnknownStep",
                "Unexpected validation error: " + throwable.getMessage()
        );
    }
}
