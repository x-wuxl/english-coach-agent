package com.wuxl.englishcoach.infrastructure.llm.dto;

import java.util.List;
import java.util.Map;

public record CoachTurnAnalysisRequest(String mode, String message, List<Map<String, Object>> recentMemory) {
}
