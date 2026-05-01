package com.wuxl.englishcoach.domain.user;

import com.wuxl.englishcoach.common.enums.GoalType;

public record UserProfile(
        Long id,
        String userCode,
        GoalType goal,
        Integer dailyMinutes,
        String studyStartTime,
        String reviewTime,
        String status
) {
}
