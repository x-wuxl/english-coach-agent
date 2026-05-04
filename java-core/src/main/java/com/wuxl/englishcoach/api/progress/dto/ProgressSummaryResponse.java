package com.wuxl.englishcoach.api.progress.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProgressSummaryResponse(
        Long userId,
        Long totalMasteryItems,
        Long dueReviewCount,
        Long completedSessionsThisWeek,
        BigDecimal recentAccuracy,
        List<ProgressWeakItemResponse> topWeakItems
) {
}
