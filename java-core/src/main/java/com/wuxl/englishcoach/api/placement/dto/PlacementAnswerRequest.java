package com.wuxl.englishcoach.api.placement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlacementAnswerRequest(
        @NotBlank String section,
        @NotBlank String questionId,
        @NotNull String result,
        String responseText,
        Integer responseTimeMs,
        Boolean hintUsed
) {
}
