package com.wuxl.englishcoach.api.user.dto;

import com.wuxl.englishcoach.common.enums.GoalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserProfileRequest(
        @NotBlank @Size(max = 64) String userCode,
        @NotNull GoalType goal,
        @NotNull @Min(5) @Max(180) Integer dailyMinutes,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "must match HH:mm") String studyStartTime,
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "must match HH:mm") String reviewTime
) {
}
