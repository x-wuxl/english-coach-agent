package com.wuxl.englishcoach.api.coach.dto;

import java.util.List;

public record FirstCoachingSessionRequest(Long userId, String goal, Integer dailyMinutes, List<String> samples) {
}
