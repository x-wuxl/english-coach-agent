package com.wuxl.englishcoach.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record CoachTurnAnalysisResponse(
        @JsonProperty("coach_reply") String coachReply,
        @JsonProperty("saved_notes") List<SavedNoteDto> savedNotes,
        @JsonProperty("expression_gaps") List<Map<String, Object>> expressionGaps,
        @JsonProperty("fix_response") Map<String, Object> fixResponse
) {
}
