package com.wuxl.englishcoach.api.mastery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MasteryStateResponse(
        Long learningItemId,
        String content,
        Integer seenCount,
        Integer correctCount,
        Integer wrongCount,
        Integer correctStreak,
        BigDecimal recognitionScore,
        BigDecimal outputScore,
        BigDecimal forgetRisk,
        String status,
        String recommendedMode,
        LocalDateTime lastSeenAt,
        LocalDateTime nextReviewAt
) {
}
