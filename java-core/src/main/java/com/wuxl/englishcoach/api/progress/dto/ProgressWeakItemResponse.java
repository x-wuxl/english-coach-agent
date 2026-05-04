package com.wuxl.englishcoach.api.progress.dto;

public record ProgressWeakItemResponse(
        Long learningItemId,
        String content,
        String meaningZh,
        Integer wrongCount,
        Integer seenCount,
        String recommendedMode
) {
}
