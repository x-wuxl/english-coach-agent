package com.wuxl.englishcoach.api.coach.dto;

import java.util.List;

public record CoachReviewResponse(
        int conversationTurns,
        int newMemoryCount,
        List<String> topRepeatedProblems,
        List<String> improvedExpressions,
        String nextWeekPlan
) {
}
