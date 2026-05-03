package com.wuxl.englishcoach.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SavedNoteDto(
        String type,
        String key,
        String label,
        @JsonProperty("description_zh") String descriptionZh,
        @JsonProperty("user_text") String userText,
        @JsonProperty("better_text") String betterText,
        String severity,
        Double confidence
) {
}
