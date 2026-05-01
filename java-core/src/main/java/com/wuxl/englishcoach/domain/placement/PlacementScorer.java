package com.wuxl.englishcoach.domain.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlacementScorer {

    private static final Map<String, Integer> BENCHMARK_MS = Map.of(
            "vocab", 4000, "grammar", 5000, "reading", 8000, "output", 12000
    );

    private static final double[][] LEVEL_THRESHOLDS = {
            {0.00, 0.19}, {0.20, 0.34}, {0.35, 0.49}, {0.50, 0.64},
            {0.65, 0.74}, {0.75, 0.84}, {0.85, 0.92}, {0.93, 1.00}
    };
    private static final String[] LEVEL_NAMES = {"A1-", "A1", "A1+", "A2", "A2+", "B1", "B1+", "B2"};

    public record SectionResult(double score, String level) {}
    public record AssessmentResult(
            String overallLevel, String vocabLevel, String grammarLevel,
            String readingLevel, String outputLevel, List<String> weaknesses,
            int suggestedNewItems, int suggestedReviewItems, int suggestedOutputTasks
    ) {}

    public record AnswerInput(String section, String result, boolean hintUsed, int responseTimeMs) {}

    public AssessmentResult assess(List<AnswerInput> answers) {
        Map<String, List<AnswerInput>> bySection = answers.stream()
                .collect(java.util.stream.Collectors.groupingBy(AnswerInput::section));

        double vocabScore = calcSectionScore(bySection.getOrDefault("vocab", List.of()), "vocab");
        double grammarScore = calcSectionScore(bySection.getOrDefault("grammar", List.of()), "grammar");
        double readingScore = calcSectionScore(bySection.getOrDefault("reading", List.of()), "reading");
        double outputScore = calcSectionScore(bySection.getOrDefault("output", List.of()), "output");

        double overallScore = 0.30 * vocabScore + 0.25 * grammarScore + 0.20 * readingScore + 0.25 * outputScore;

        String overallLevel = scoreToLevel(overallScore);
        String vocabLevel = scoreToLevel(vocabScore);
        String grammarLevel = scoreToLevel(grammarScore);
        String readingLevel = scoreToLevel(readingScore);
        String outputLevel = scoreToLevel(outputScore);

        List<String> weaknesses = extractWeaknesses(vocabScore, grammarScore, outputScore, overallScore);
        int[] rhythm = suggestDailyRhythm(overallLevel, outputLevel, overallScore, outputScore);

        return new AssessmentResult(overallLevel, vocabLevel, grammarLevel, readingLevel, outputLevel,
                weaknesses, rhythm[0], rhythm[1], rhythm[2]);
    }

    private double calcSectionScore(List<AnswerInput> answers, String section) {
        if (answers.isEmpty()) return 0.0;

        int total = answers.size();
        long correct = answers.stream().filter(a -> "CORRECT".equals(a.result())).count();
        long hintUsed = answers.stream().filter(AnswerInput::hintUsed).count();

        double accuracy = (double) correct / total;
        double hintPenalty = (double) hintUsed / total * 0.10;

        double avgTime = answers.stream().mapToInt(AnswerInput::responseTimeMs).average().orElse(0);
        int benchmark = BENCHMARK_MS.getOrDefault(section, 5000);
        double slowPenalty = Math.min(avgTime / benchmark - 1, 1) * 0.05;
        slowPenalty = Math.max(slowPenalty, 0);

        return Math.max(0, Math.min(1, accuracy - hintPenalty - slowPenalty));
    }

    public String scoreToLevel(double score) {
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (score >= LEVEL_THRESHOLDS[i][0] && score <= LEVEL_THRESHOLDS[i][1]) {
                return LEVEL_NAMES[i];
            }
        }
        return "B2";
    }

    private List<String> extractWeaknesses(double vocab, double grammar, double output, double overall) {
        List<String> weaknesses = new ArrayList<>();
        if (output < 0.50) weaknesses.add("OUTPUT_WEAKNESS");
        if (grammar < 0.50) weaknesses.add("GRAMMAR_UNSTABLE");
        if (vocab < 0.50) weaknesses.add("VOCAB_WEAKNESS");
        if (vocab > 0.60 && output < vocab - 0.20) weaknesses.add("KNOW_BUT_CANNOT_USE");
        return weaknesses.subList(0, Math.min(3, weaknesses.size()));
    }

    private int[] suggestDailyRhythm(String overallLevel, String outputLevel, double overallScore, double outputScore) {
        int newItems, reviewItems, outputTasks;

        if (overallScore < 0.50) {
            newItems = 5; reviewItems = 10; outputTasks = 1;
        } else if (overallScore < 0.65) {
            newItems = 7; reviewItems = 12; outputTasks = 2;
        } else {
            newItems = 8; reviewItems = 13; outputTasks = 3;
        }

        if (outputScore < overallScore - 0.15) {
            outputTasks += 1;
        }

        return new int[]{newItems, reviewItems, outputTasks};
    }
}
