package com.wuxl.englishcoach.api.coach.dto;

import com.wuxl.englishcoach.api.memory.dto.PriorityMemoryResponse;

public record FirstCoachingSessionResponse(
        Long sessionId,
        String sessionCode,
        String detectedLevelRange,
        PriorityMemoryResponse initialMemory
) {
}
