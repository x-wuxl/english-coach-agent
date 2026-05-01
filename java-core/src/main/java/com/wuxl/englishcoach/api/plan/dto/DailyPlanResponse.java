package com.wuxl.englishcoach.api.plan.dto;

import java.util.List;

public record DailyPlanResponse(
        String planCode,
        String planDate,
        String planType,
        String status,
        List<DailyPlanItemResponse> newItems,
        List<DailyPlanItemResponse> reviewItems,
        DailyPlanRationaleResponse rationale
) {
}
