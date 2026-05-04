package com.wuxl.englishcoach.api.plan.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DailyPlanItemResponse(
        Long itemId,
        String itemCode,
        String type,
        String content,
        String meaningZh,
        Integer difficulty,
        String theme,
        List<Map<String, String>> examples,
        String itemRole,
        String recommendedMode,
        BigDecimal priorityScore,
        String selectionReason
) {
}
