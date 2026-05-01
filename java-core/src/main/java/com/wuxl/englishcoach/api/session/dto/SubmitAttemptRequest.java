package com.wuxl.englishcoach.api.session.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitAttemptRequest(
        @NotNull Long learningItemId,
        @NotNull String mode,
        @NotNull String result,
        String responseText,
        Integer responseTimeMs,
        Boolean hintUsed,
        String errorType
) {
}
