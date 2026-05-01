package com.wuxl.englishcoach.api.review.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateWeeklyReviewRequest(
        @NotNull Long userId,
        @NotNull String weekStartDate,
        @NotNull String weekEndDate
) {
}
