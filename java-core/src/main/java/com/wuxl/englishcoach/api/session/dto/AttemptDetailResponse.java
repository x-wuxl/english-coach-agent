package com.wuxl.englishcoach.api.session.dto;

public record AttemptDetailResponse(
        Long id,
        String attemptCode,
        Long learningItemId,
        String mode,
        String result,
        String responseText,
        Integer responseTimeMs,
        Boolean hintUsed,
        String errorType,
        String llmExplanation
) {
}
