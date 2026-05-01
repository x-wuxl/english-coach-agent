package com.wuxl.englishcoach.api.session.dto;

public record StudySessionStartResponse(
        Long id,
        String sessionCode,
        String sessionType,
        String status
) {
}
