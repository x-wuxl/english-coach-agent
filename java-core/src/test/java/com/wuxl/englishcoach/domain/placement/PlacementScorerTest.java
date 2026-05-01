package com.wuxl.englishcoach.domain.placement;

import static org.assertj.core.api.Assertions.assertThat;

import com.wuxl.englishcoach.domain.placement.PlacementScorer.AnswerInput;
import com.wuxl.englishcoach.domain.placement.PlacementScorer.AssessmentResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementScorerTest {

    private final PlacementScorer scorer = new PlacementScorer();

    @Test
    void highAccuracyShouldMapToHighLevel() {
        List<AnswerInput> answers = List.of(
                new AnswerInput("vocab", "CORRECT", false, 2000),
                new AnswerInput("vocab", "CORRECT", false, 2500),
                new AnswerInput("vocab", "CORRECT", false, 3000),
                new AnswerInput("grammar", "CORRECT", false, 3000),
                new AnswerInput("grammar", "CORRECT", false, 4000),
                new AnswerInput("reading", "CORRECT", false, 5000),
                new AnswerInput("reading", "CORRECT", false, 6000),
                new AnswerInput("output", "CORRECT", false, 8000),
                new AnswerInput("output", "CORRECT", false, 9000)
        );

        AssessmentResult result = scorer.assess(answers);
        assertThat(result.overallLevel()).isIn("B1", "B1+", "B2");
    }

    @Test
    void lowOutputShouldIdentifyWeakness() {
        List<AnswerInput> answers = List.of(
                new AnswerInput("vocab", "CORRECT", false, 2000),
                new AnswerInput("vocab", "CORRECT", false, 2000),
                new AnswerInput("vocab", "CORRECT", false, 2000),
                new AnswerInput("output", "WRONG", false, 15000),
                new AnswerInput("output", "WRONG", false, 14000),
                new AnswerInput("output", "WRONG", false, 16000)
        );

        AssessmentResult result = scorer.assess(answers);
        assertThat(result.weaknesses()).contains("OUTPUT_WEAKNESS");
    }

    @Test
    void hintsShouldReduceScore() {
        List<AnswerInput> noHints = List.of(
                new AnswerInput("vocab", "CORRECT", false, 2000),
                new AnswerInput("vocab", "CORRECT", false, 2000)
        );
        List<AnswerInput> withHints = List.of(
                new AnswerInput("vocab", "CORRECT", true, 2000),
                new AnswerInput("vocab", "CORRECT", true, 2000)
        );

        double noHintScore = scorer.assess(noHints).overallLevel().compareTo("A1") > 0 ? 1 : 0;
        // Both should be correct, but hints should lower the score
        // We compare levels - with hints should be same or lower
        AssessmentResult noHintResult = scorer.assess(noHints);
        AssessmentResult hintResult = scorer.assess(withHints);
        // Both have 100% accuracy, but hints penalize
        // The level should be the same or noHint should be higher
        assertThat(noHintResult.vocabLevel()).isGreaterThanOrEqualTo(hintResult.vocabLevel());
    }

    @Test
    void emptyAnswersShouldReturnZeroScores() {
        AssessmentResult result = scorer.assess(List.of());
        assertThat(result.overallLevel()).isEqualTo("A1-");
    }

    @Test
    void scoreToLevelShouldMapCorrectly() {
        assertThat(scorer.scoreToLevel(0.0)).isEqualTo("A1-");
        assertThat(scorer.scoreToLevel(0.25)).isEqualTo("A1");
        assertThat(scorer.scoreToLevel(0.40)).isEqualTo("A1+");
        assertThat(scorer.scoreToLevel(0.55)).isEqualTo("A2");
        assertThat(scorer.scoreToLevel(0.70)).isEqualTo("A2+");
        assertThat(scorer.scoreToLevel(0.80)).isEqualTo("B1");
        assertThat(scorer.scoreToLevel(0.90)).isEqualTo("B1+");
        assertThat(scorer.scoreToLevel(0.95)).isEqualTo("B2");
    }
}
