package com.wuxl.englishcoach.api.session.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudySessionDetailResponse(
        Long id,
        String sessionCode,
        Long userId,
        String sessionType,
        String status,
        Integer durationMin,
        Integer newItemsCount,
        Integer reviewItemsCount,
        BigDecimal accuracy,
        BigDecimal completionRate,
        List<AttemptDetailResponse> attempts
) {
}
