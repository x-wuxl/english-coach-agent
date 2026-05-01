package com.wuxl.englishcoach.domain.mastery;

import static org.assertj.core.api.Assertions.assertThat;

import com.wuxl.englishcoach.domain.mastery.ScoreUpdatePolicy.ScoreDelta;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoreUpdatePolicyTest {

    private final ScoreUpdatePolicy policy = new ScoreUpdatePolicy();

    @Test
    void correctAnswerShouldIncreaseRecognition() {
        ScoreDelta delta = policy.calculate(false, false, "recognition_quiz");
        assertThat(delta.recognitionDelta()).isLessThan(BigDecimal.ZERO);

        ScoreDelta correctDelta = policy.calculate(true, false, "recognition_quiz");
        assertThat(correctDelta.recognitionDelta()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void hintCorrectShouldGiveLessBoost() {
        ScoreDelta noHint = policy.calculate(true, false, "recognition_quiz");
        ScoreDelta withHint = policy.calculate(true, true, "recognition_quiz");
        assertThat(noHint.recognitionDelta()).isGreaterThan(withHint.recognitionDelta());
    }

    @Test
    void outputModeCorrectShouldBoostOutputMore() {
        ScoreDelta outputCorrect = policy.calculate(true, false, "cn_to_en");
        ScoreDelta recognitionCorrect = policy.calculate(true, false, "recognition_quiz");
        assertThat(outputCorrect.outputDelta()).isGreaterThan(recognitionCorrect.outputDelta());
    }

    @Test
    void wrongAnswerShouldDecreaseScores() {
        ScoreDelta delta = policy.calculate(false, false, "cn_to_en");
        assertThat(delta.recognitionDelta()).isLessThan(BigDecimal.ZERO);
        assertThat(delta.outputDelta()).isLessThan(BigDecimal.ZERO);
        assertThat(delta.forgetRiskDelta()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void correctAnswerShouldDecreaseForgetRisk() {
        ScoreDelta delta = policy.calculate(true, false, "recognition_quiz");
        assertThat(delta.forgetRiskDelta()).isLessThan(BigDecimal.ZERO);
    }
}
