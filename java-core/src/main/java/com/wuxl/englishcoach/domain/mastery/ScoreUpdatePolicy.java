package com.wuxl.englishcoach.domain.mastery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ScoreUpdatePolicy {

    public record ScoreDelta(BigDecimal recognitionDelta, BigDecimal outputDelta, BigDecimal forgetRiskDelta) {}

    public ScoreDelta calculate(boolean correct, boolean hintUsed, String mode) {
        boolean isOutputMode = isOutputMode(mode);

        BigDecimal recognitionDelta;
        BigDecimal outputDelta;

        if (correct) {
            recognitionDelta = hintUsed ? BigDecimal.valueOf(0.04) : BigDecimal.valueOf(0.10);
            outputDelta = isOutputMode ? BigDecimal.valueOf(0.15) : BigDecimal.valueOf(0.05);
        } else {
            recognitionDelta = BigDecimal.valueOf(-0.08);
            outputDelta = isOutputMode ? BigDecimal.valueOf(-0.12) : BigDecimal.valueOf(-0.04);
        }

        // forget_risk: correct → slight decrease, wrong → increase
        BigDecimal forgetRiskDelta = correct ? BigDecimal.valueOf(-0.05) : BigDecimal.valueOf(0.08);

        return new ScoreDelta(
                recognitionDelta.setScale(2, RoundingMode.HALF_UP),
                outputDelta.setScale(2, RoundingMode.HALF_UP),
                forgetRiskDelta.setScale(2, RoundingMode.HALF_UP));
    }

    private boolean isOutputMode(String mode) {
        if (mode == null) return false;
        return switch (mode) {
            case "cn_to_en", "sentence_building", "scenario_response" -> true;
            default -> false;
        };
    }
}
