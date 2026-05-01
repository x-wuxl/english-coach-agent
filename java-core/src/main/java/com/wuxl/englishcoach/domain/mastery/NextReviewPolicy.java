package com.wuxl.englishcoach.domain.mastery;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class NextReviewPolicy {

    private static final int[] BASE_INTERVALS_DAYS = {1, 3, 7, 14, 30};

    public LocalDateTime calculateNextReview(String currentStatus, int correctStreak, boolean lastCorrect) {
        int intervalDays;

        if (!lastCorrect) {
            intervalDays = 1; // reset on error
        } else {
            int idx = Math.min(correctStreak, BASE_INTERVALS_DAYS.length - 1);
            intervalDays = BASE_INTERVALS_DAYS[idx];
        }

        // WEAK_OUTPUT items get shorter intervals
        if ("WEAK_OUTPUT".equals(currentStatus)) {
            intervalDays = Math.max(1, intervalDays / 2);
        }

        return LocalDateTime.now().plusDays(intervalDays);
    }
}
