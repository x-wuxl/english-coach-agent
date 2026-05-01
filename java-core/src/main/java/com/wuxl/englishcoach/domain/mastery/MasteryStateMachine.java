package com.wuxl.englishcoach.domain.mastery;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MasteryStateMachine {

    public String transition(String currentStatus, BigDecimal recognitionScore, BigDecimal outputScore,
                             int correctStreak, int recentConsecutiveErrors) {
        if (currentStatus == null) return "NEW";

        return switch (currentStatus) {
            case "NEW" -> "LEARNING";
            case "LEARNING" -> {
                if (recognitionScore.doubleValue() >= 0.70 && outputScore.doubleValue() < 0.45) {
                    yield "WEAK_OUTPUT";
                }
                if (recognitionScore.doubleValue() >= 0.70 && correctStreak >= 2) {
                    yield "REVIEWING";
                }
                yield "LEARNING";
            }
            case "REVIEWING" -> {
                if (recognitionScore.doubleValue() >= 0.70 && outputScore.doubleValue() < 0.45) {
                    yield "WEAK_OUTPUT";
                }
                if (recognitionScore.doubleValue() >= 0.85 && outputScore.doubleValue() >= 0.70 && correctStreak >= 4) {
                    yield "MASTERED";
                }
                yield "REVIEWING";
            }
            case "WEAK_OUTPUT" -> {
                if (recognitionScore.doubleValue() >= 0.85 && outputScore.doubleValue() >= 0.70 && correctStreak >= 4) {
                    yield "MASTERED";
                }
                if (recognitionScore.doubleValue() >= 0.70 && correctStreak >= 2) {
                    yield "REVIEWING";
                }
                yield "WEAK_OUTPUT";
            }
            case "MASTERED" -> {
                if (recentConsecutiveErrors >= 2) {
                    yield outputScore.doubleValue() < 0.45 ? "WEAK_OUTPUT" : "REVIEWING";
                }
                yield "MASTERED";
            }
            default -> currentStatus;
        };
    }
}
