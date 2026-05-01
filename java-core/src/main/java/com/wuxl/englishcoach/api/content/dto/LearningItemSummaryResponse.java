package com.wuxl.englishcoach.api.content.dto;

public record LearningItemSummaryResponse(
        Long id,
        String itemCode,
        String type,
        String content,
        String meaningZh,
        Integer difficulty,
        String theme,
        String status
) {
}
