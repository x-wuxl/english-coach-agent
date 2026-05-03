package com.wuxl.englishcoach.api.memory.dto;

import java.time.LocalDateTime;

public record PriorityMemoryItemResponse(
        String memoryType,
        Long memoryId,
        String label,
        String sourceText,
        String betterText,
        Integer seenCount,
        String status,
        String recommendedAction,
        LocalDateTime nextDrillAt,
        double priorityScore
) {
}
