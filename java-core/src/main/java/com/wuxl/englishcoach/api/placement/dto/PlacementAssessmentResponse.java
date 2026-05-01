package com.wuxl.englishcoach.api.placement.dto;

import java.util.List;

public record PlacementAssessmentResponse(
        String overallLevel,
        String vocabLevel,
        String grammarLevel,
        String readingLevel,
        String outputLevel,
        List<String> weaknesses,
        SuggestedDailyRhythmResponse suggestedDailyRhythm
) {
}
