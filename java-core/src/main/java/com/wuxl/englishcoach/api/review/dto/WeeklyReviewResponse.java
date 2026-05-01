package com.wuxl.englishcoach.api.review.dto;

import java.math.BigDecimal;
import java.util.List;

public record WeeklyReviewResponse(
        Long userId,
        String weekStartDate,
        String weekEndDate,
        BigDecimal completionRate,
        int studyMinutes,
        int newItemsCount,
        int reviewItemsCount,
        List<String> highFrequencyErrorTypes,
        List<String> strongestThemes,
        List<String> weakestThemes,
        String nextWeekSuggestion
) {
}
