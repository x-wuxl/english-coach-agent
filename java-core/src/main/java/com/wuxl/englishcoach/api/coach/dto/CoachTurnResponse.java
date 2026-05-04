package com.wuxl.englishcoach.api.coach.dto;

import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryResponse;
import java.util.List;
import java.util.Map;

public record CoachTurnResponse(
        String coachReply,
        List<SavedNoteResponse> savedNotes,
        PriorityMemoryResponse priorityMemory,
        DrillSuggestionResponse drillSuggestion,
        Map<String, Object> fixResponse
) {
}
