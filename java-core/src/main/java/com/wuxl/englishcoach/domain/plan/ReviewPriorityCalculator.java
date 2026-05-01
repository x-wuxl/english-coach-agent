package com.wuxl.englishcoach.domain.plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ReviewPriorityCalculator {

    public record MasterySnapshot(
            Long learningItemId,
            BigDecimal recognitionScore,
            BigDecimal outputScore,
            BigDecimal forgetRisk,
            LocalDateTime lastSeenAt,
            LocalDateTime nextReviewAt,
            String status,
            Integer correctCount,
            Integer wrongCount
    ) {}

    public double calculatePriority(MasterySnapshot snapshot) {
        double dueScore = calcDueScore(snapshot.nextReviewAt());
        double forgetRisk = snapshot.forgetRisk() != null ? snapshot.forgetRisk().doubleValue() : 0.5;
        double recentErrorScore = calcRecentErrorScore(snapshot.correctCount(), snapshot.wrongCount());
        double outputGap = calcOutputGap(snapshot.recognitionScore(), snapshot.outputScore());
        double themeWeight = 0.5; // default neutral
        double exposurePenalty = calcExposurePenalty(snapshot.lastSeenAt());

        double priority = 0.30 * dueScore
                + 0.25 * forgetRisk
                + 0.20 * recentErrorScore
                + 0.15 * outputGap
                + 0.10 * themeWeight
                - 0.10 * exposurePenalty;

        return Math.max(0, Math.min(1, priority));
    }

    private double calcDueScore(LocalDateTime nextReviewAt) {
        if (nextReviewAt == null) return 0.6;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(nextReviewAt)) return 0.2;
        long overdueHours = ChronoUnit.HOURS.between(nextReviewAt, now);
        if (overdueHours < 24) return 0.7;
        return 1.0;
    }

    private double calcRecentErrorScore(Integer correctCount, Integer wrongCount) {
        int correct = correctCount != null ? correctCount : 0;
        int wrong = wrongCount != null ? wrongCount : 0;
        int total = correct + wrong;
        if (total < 2) return 0.0;
        return (double) wrong / total;
    }

    private double calcOutputGap(BigDecimal recognitionScore, BigDecimal outputScore) {
        double recognition = recognitionScore != null ? recognitionScore.doubleValue() : 0.0;
        double output = outputScore != null ? outputScore.doubleValue() : 0.0;
        return Math.max(recognition - output, 0);
    }

    private double calcExposurePenalty(LocalDateTime lastSeenAt) {
        if (lastSeenAt == null) return 0.0;
        long hoursAgo = ChronoUnit.HOURS.between(lastSeenAt, LocalDateTime.now());
        if (hoursAgo < 1) return 0.8;
        if (hoursAgo < 6) return 0.4;
        if (hoursAgo < 24) return 0.2;
        return 0.0;
    }
}
