package com.wuxl.englishcoach.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record CoachTurnAnalysisRequest(
        String mode,
        String message,
        @JsonProperty("recent_messages")
        List<String> recentMessages,
        @JsonProperty("learner_context")
        Map<String, Object> learnerContext) {
}
