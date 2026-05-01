package com.wuxl.englishcoach.api.user.dto;

public record UserProfileResponse(
        Long id,
        String userCode,
        String goal,
        Integer dailyMinutes,
        String studyStartTime,
        String reviewTime,
        String status
) {
}
