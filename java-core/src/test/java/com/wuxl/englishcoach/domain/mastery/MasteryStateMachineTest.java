package com.wuxl.englishcoach.domain.mastery;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MasteryStateMachineTest {

    private final MasteryStateMachine machine = new MasteryStateMachine();

    @Test
    void newShouldTransitionToLearning() {
        String result = machine.transition("NEW", BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3), 0, 0);
        assertThat(result).isEqualTo("LEARNING");
    }

    @Test
    void learningWithHighRecognitionAndStreakShouldTransitionToReviewing() {
        String result = machine.transition("LEARNING", BigDecimal.valueOf(0.75), BigDecimal.valueOf(0.5), 3, 0);
        assertThat(result).isEqualTo("REVIEWING");
    }

    @Test
    void learningWithHighRecognitionLowOutputShouldTransitionToWeakOutput() {
        String result = machine.transition("LEARNING", BigDecimal.valueOf(0.75), BigDecimal.valueOf(0.30), 0, 0);
        assertThat(result).isEqualTo("WEAK_OUTPUT");
    }

    @Test
    void learningWithLowRecognitionShouldStayLearning() {
        String result = machine.transition("LEARNING", BigDecimal.valueOf(0.50), BigDecimal.valueOf(0.30), 0, 0);
        assertThat(result).isEqualTo("LEARNING");
    }

    @Test
    void reviewingShouldTransitionToMasteredWhenConditionsMet() {
        String result = machine.transition("REVIEWING", BigDecimal.valueOf(0.90), BigDecimal.valueOf(0.75), 5, 0);
        assertThat(result).isEqualTo("MASTERED");
    }

    @Test
    void reviewingWithHighRecognitionLowOutputShouldGoToWeakOutput() {
        String result = machine.transition("REVIEWING", BigDecimal.valueOf(0.80), BigDecimal.valueOf(0.30), 2, 0);
        assertThat(result).isEqualTo("WEAK_OUTPUT");
    }

    @Test
    void masteredWithConsecutiveErrorsShouldRegress() {
        String result = machine.transition("MASTERED", BigDecimal.valueOf(0.60), BigDecimal.valueOf(0.50), 0, 3);
        assertThat(result).isEqualTo("REVIEWING");
    }

    @Test
    void masteredWithOutputDropShouldRegressToWeakOutput() {
        String result = machine.transition("MASTERED", BigDecimal.valueOf(0.60), BigDecimal.valueOf(0.30), 0, 3);
        assertThat(result).isEqualTo("WEAK_OUTPUT");
    }

    @Test
    void masteredWithOccasionalErrorShouldStay() {
        String result = machine.transition("MASTERED", BigDecimal.valueOf(0.90), BigDecimal.valueOf(0.80), 0, 1);
        assertThat(result).isEqualTo("MASTERED");
    }

    @Test
    void weakOutputShouldRecoverToReviewing() {
        String result = machine.transition("WEAK_OUTPUT", BigDecimal.valueOf(0.75), BigDecimal.valueOf(0.50), 3, 0);
        assertThat(result).isEqualTo("REVIEWING");
    }
}
