package com.wuxl.englishcoach.api.coach.dto;

public record CoachSessionResponse(Long id, String sessionCode, Long userId, String sessionType, String status) {
}
