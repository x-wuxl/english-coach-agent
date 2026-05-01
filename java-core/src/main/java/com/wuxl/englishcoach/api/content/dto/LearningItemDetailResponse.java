package com.wuxl.englishcoach.api.content.dto;

import java.util.List;
import java.util.Map;

public record LearningItemDetailResponse(
        Long id,
        String itemCode,
        String type,
        String content,
        String meaningZh,
        Integer difficulty,
        String theme,
        List<String> tags,
        List<Map<String, String>> examples,
        List<String> relatedItemCodes,
        String status,
        String createdAt,
        String updatedAt
) {
}
