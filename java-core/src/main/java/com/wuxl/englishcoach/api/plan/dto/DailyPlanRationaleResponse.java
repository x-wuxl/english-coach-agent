package com.wuxl.englishcoach.api.plan.dto;

import java.util.List;

public record DailyPlanRationaleResponse(
        String loadDecision,
        List<String> whyReviewThese,
        String whyNewCountIsThis
) {
}
