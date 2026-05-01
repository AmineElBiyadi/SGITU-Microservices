package com.ensate.billetterie.validation.steps;

import com.ensate.billetterie.validation.domain.ValidationContext;
import com.ensate.billetterie.validation.interfaces.NextStep;
import com.ensate.billetterie.validation.interfaces.ValidationStep;

import java.util.concurrent.CompletableFuture;

//This is an example. Feel free to delete it
//Create other Classes like this one here
public class TicketStatusStep implements ValidationStep {

    @Override
    public String getStepName() {
        return "";
    }

    @Override
    public CompletableFuture<ValidationContext> execute(ValidationContext context, NextStep next) {
        return null;
    }
}
