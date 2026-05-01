package com.wuxl.englishcoach.api.placement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlacementAssessmentRequest(
        @NotNull Long userId,
        @Valid List<PlacementAnswerRequest> answers
) {
}
