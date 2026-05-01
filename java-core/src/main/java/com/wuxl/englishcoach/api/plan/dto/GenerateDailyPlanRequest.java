package com.wuxl.englishcoach.api.plan.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateDailyPlanRequest(
        @NotNull Long userId,
        @NotNull String planDate,
        String planType
) {
}
