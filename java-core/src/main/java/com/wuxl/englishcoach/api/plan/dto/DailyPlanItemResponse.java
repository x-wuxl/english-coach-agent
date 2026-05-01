package com.wuxl.englishcoach.api.plan.dto;

import java.math.BigDecimal;

public record DailyPlanItemResponse(
        Long itemId,
        String content,
        String itemRole,
        String recommendedMode,
        BigDecimal priorityScore,
        String selectionReason
) {
}
