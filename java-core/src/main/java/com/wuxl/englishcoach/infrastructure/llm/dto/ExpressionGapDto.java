package com.wuxl.englishcoach.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ExpressionGapDto(
        String key,
        @JsonProperty("zh_intent") String zhIntent,
        @JsonProperty("natural_expressions") List<String> naturalExpressions,
        @JsonProperty("user_attempt") String userAttempt,
        String context,
        Double confidence
) {
}
