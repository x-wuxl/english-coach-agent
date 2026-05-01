package com.wuxl.englishcoach.api.user.dto;

import java.util.List;

public record UserProfileResponse(
        Long id,
        String userCode,
        String goal,
        List<String> subGoals,
        Integer dailyMinutes,
        String studyStartTime,
        String reviewTime,
        String overallLevel,
        String vocabLevel,
        String grammarLevel,
        String readingLevel,
        String outputLevel,
        List<String> preferredModes,
        String motivationStyle,
        String fatigueTolerance,
        String status
) {
}
