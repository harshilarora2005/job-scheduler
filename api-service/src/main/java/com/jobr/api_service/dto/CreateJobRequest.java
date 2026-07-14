package com.jobr.api_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
        @NotBlank String jobType,
        @NotNull Object payload,
        Short priority,
        String cronExpression,
        Boolean isRecurring,
        String idempotencyKey
) {}