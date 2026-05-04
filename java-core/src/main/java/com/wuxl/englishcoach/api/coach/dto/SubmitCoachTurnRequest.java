package com.wuxl.englishcoach.api.coach.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitCoachTurnRequest(
        @NotBlank String mode,
        @NotBlank String message) {
}
