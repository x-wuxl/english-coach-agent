package com.wuxl.englishcoach.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import com.wuxl.englishcoach.domain.plan.DailyLoadPolicy.LoadResult;
import org.junit.jupiter.api.Test;

class DailyLoadPolicyTest {

    private final DailyLoadPolicy policy = new DailyLoadPolicy();

    @Test
    void baseTierForShortSession() {
        LoadResult result = policy.calculate(8, 0.5, 0.5, 0.0, 0);
        assertThat(result.newCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.reviewCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.loadDecision()).isIn("LIGHT", "NORMAL", "BOOST");
    }

    @Test
    void baseTierForMediumSession() {
        LoadResult result = policy.calculate(15, 0.5, 0.5, 0.0, 0);
        assertThat(result.newCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.reviewCount()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void highCompletionBoostsLoad() {
        LoadResult normal = policy.calculate(15, 0.5, 0.5, 0.0, 0);
        LoadResult boosted = policy.calculate(15, 0.90, 0.5, 0.0, 0);
        assertThat(boosted.newCount()).isGreaterThan(normal.newCount());
    }

    @Test
    void highAccuracyBoostsNew() {
        LoadResult normal = policy.calculate(15, 0.5, 0.5, 0.0, 0);
        LoadResult accurate = policy.calculate(15, 0.5, 0.90, 0.0, 0);
        assertThat(accurate.newCount()).isGreaterThan(normal.newCount());
    }

    @Test
    void fatigueReducesLoad() {
        LoadResult normal = policy.calculate(15, 0.5, 0.5, 0.0, 0);
        LoadResult fatigued = policy.calculate(15, 0.5, 0.5, 0.6, 0);
        assertThat(fatigued.newCount()).isLessThan(normal.newCount());
    }

    @Test
    void lowCompletionReducesLoad() {
        LoadResult normal = policy.calculate(15, 0.5, 0.5, 0.0, 0);
        LoadResult low = policy.calculate(15, 0.30, 0.5, 0.0, 0);
        assertThat(low.newCount()).isLessThan(normal.newCount());
    }

    @Test
    void manyOverdueIncreasesReview() {
        LoadResult normal = policy.calculate(15, 0.5, 0.5, 0.0, 0);
        LoadResult overdue = policy.calculate(15, 0.5, 0.5, 0.0, 15);
        assertThat(overdue.reviewCount()).isGreaterThan(normal.reviewCount());
    }

    @Test
    void minimumCountsNeverDropBelowOne() {
        // Extreme fatigue + low completion + short session
        LoadResult result = policy.calculate(5, 0.2, 0.3, 0.8, 0);
        assertThat(result.newCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.reviewCount()).isGreaterThanOrEqualTo(1);
    }
}
