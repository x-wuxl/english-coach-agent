package com.wuxl.englishcoach.domain.user;

import com.wuxl.englishcoach.common.enums.GoalType;
import java.util.List;

public record UserProfile(
        Long id,
        String userCode,
        GoalType goal,
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
