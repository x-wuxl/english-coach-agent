package com.wuxl.englishcoach.domain.content;

import java.util.List;
import java.util.Map;

public record LearningItem(
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
        String status
) {
}
