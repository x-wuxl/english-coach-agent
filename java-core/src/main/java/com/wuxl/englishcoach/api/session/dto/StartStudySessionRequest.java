package com.wuxl.englishcoach.api.session.dto;

import jakarta.validation.constraints.NotNull;

public record StartStudySessionRequest(
        @NotNull Long userId,
        String sessionType,
        String focusTheme
) {
}
