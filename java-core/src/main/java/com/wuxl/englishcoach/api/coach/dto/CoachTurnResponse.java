package com.wuxl.englishcoach.api.coach.dto;

import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryResponse;
import java.util.List;

public record CoachTurnResponse(
        String coachReply,
        List<SavedNoteResponse> savedNotes,
        PriorityMemoryResponse priorityMemory,
        DrillSuggestionResponse drillSuggestion
) {
}
