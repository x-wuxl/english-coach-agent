package com.wuxl.englishcoach.api.plan.dto;

import java.math.BigDecimal;

public record DailyPlanItemResponse(
        Long itemId,
        String content,
        String meaningZh,
        String itemRole,
        String recommendedMode,
        BigDecimal priorityScore,
        String selectionReason
) {
}
