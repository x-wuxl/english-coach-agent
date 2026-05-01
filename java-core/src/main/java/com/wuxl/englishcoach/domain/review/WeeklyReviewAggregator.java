package com.wuxl.englishcoach.domain.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReviewAggregator {

    public record SessionData(int durationMin, BigDecimal accuracy, String status, String sessionType) {}
    public record AttemptData(String result, String errorType, String theme) {}

    public record ReviewResult(
            BigDecimal completionRate,
            int studyMinutes,
            int newItemsCount,
            int reviewItemsCount,
            List<String> highFrequencyErrorTypes,
            List<String> strongestThemes,
            List<String> weakestThemes,
            String nextWeekSuggestion
    ) {}

    public ReviewResult aggregate(List<SessionData> sessions, List<AttemptData> attempts,
                                   int generatedPlanCount, int completedPlanCount) {
        // Completion rate
        BigDecimal completionRate = generatedPlanCount > 0 ?
                BigDecimal.valueOf((double) completedPlanCount / generatedPlanCount).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Study minutes
        int studyMinutes = sessions.stream().mapToInt(SessionData::durationMin).sum();

        // Item counts from session types
        int newCount = (int) sessions.stream().filter(s -> "DAILY_LEARNING".equals(s.sessionType())).count();
        int reviewCount = (int) sessions.stream().filter(s -> !"PLACEMENT".equals(s.sessionType())).count();

        // High frequency error types
        Map<String, Long> errorCounts = attempts.stream()
                .filter(a -> a.errorType() != null && !"CORRECT".equals(a.result()))
                .collect(Collectors.groupingBy(AttemptData::errorType, Collectors.counting()));
        List<String> topErrors = errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // Theme analysis
        Map<String, int[]> themeStats = new HashMap<>();
        for (AttemptData a : attempts) {
            if (a.theme() == null) continue;
            int[] stats = themeStats.computeIfAbsent(a.theme(), k -> new int[]{0, 0}); // [correct, total]
            stats[1]++;
            if ("CORRECT".equals(a.result())) stats[0]++;
        }

        List<Map.Entry<String, double[]>> scoredThemes = themeStats.entrySet().stream()
                .map(e -> {
                    double accuracy = e.getValue()[1] > 0 ? (double) e.getValue()[0] / e.getValue()[1] : 0;
                    double errorRate = 1.0 - accuracy;
                    double score = 0.5 * accuracy + 0.3 * accuracy - 0.2 * errorRate;
                    return Map.entry(e.getKey(), new double[]{score, accuracy});
                })
                .sorted(Comparator.comparingDouble((Map.Entry<String, double[]> e) -> e.getValue()[0]).reversed())
                .toList();

        List<String> strongest = scoredThemes.stream().limit(2).map(Map.Entry::getKey).toList();
        List<String> weakest = scoredThemes.stream()
                .skip(Math.max(0, scoredThemes.size() - 2))
                .map(Map.Entry::getKey)
                .toList();

        // Next week suggestion
        double avgAccuracy = attempts.isEmpty() ? 0 :
                attempts.stream().filter(a -> "CORRECT".equals(a.result())).count() / (double) attempts.size();
        String loadSuggestion = "keep";
        if (completionRate.doubleValue() >= 0.85 && avgAccuracy >= 0.80) {
            loadSuggestion = "increase";
        } else if (completionRate.doubleValue() < 0.50 || avgAccuracy < 0.40) {
            loadSuggestion = "reduce";
        }

        String suggestion = String.format("{\"load\":\"%s\",\"outputRatio\":\"keep\"}", loadSuggestion);

        return new ReviewResult(completionRate, studyMinutes, newCount, reviewCount,
                topErrors, strongest, weakest, suggestion);
    }
}
