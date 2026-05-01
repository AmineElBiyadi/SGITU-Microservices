package com.ensate.billetterie.validation.pipeline;


import com.ensate.billetterie.ticket.dto.result.ValidationResult;
import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ValidationPipeline {

    private final List<ValidationStep> steps = new ArrayList<>();

    public ValidationPipeline addStep(ValidationStep step) {
        steps.add(step);
        return this;
    }

    public CompletableFuture<ValidationResult> execute(ValidationContext context) {
        NextStep runner = buildChain(0);

        return runner.execute(context)
                .thenApply(this::buildResult);
    }

    private NextStep buildChain(int index) {
        if (index >= steps.size()) {
            return CompletableFuture::completedFuture;
        }

        return ctx -> {
            if (ctx.isDenied()) {
                return CompletableFuture.completedFuture(ctx);
            }

            ValidationStep currentStep = steps.get(index);
            return currentStep.execute(ctx, buildChain(index + 1));
        };
    }

    private ValidationResult buildResult(ValidationContext context) {
        return new ValidationResult(context.isDenied(), context.getMessage());
    }
}
