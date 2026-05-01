package com.wuxl.englishcoach.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;

import com.wuxl.englishcoach.domain.plan.ReviewPriorityCalculator.MasterySnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReviewPriorityCalculatorTest {

    private final ReviewPriorityCalculator calculator = new ReviewPriorityCalculator();

    @Test
    void overdueItemHasHighPriority() {
        MasterySnapshot overdue = new MasterySnapshot(
                1L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(0.6), LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2), "LEARNING", 3, 5);

        MasterySnapshot notDue = new MasterySnapshot(
                2L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(0.6), LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(2), "LEARNING", 3, 5);

        double overduePriority = calculator.calculatePriority(overdue);
        double notDuePriority = calculator.calculatePriority(notDue);

        assertThat(overduePriority).isGreaterThan(notDuePriority);
    }

    @Test
    void highForgetRiskIncreasesPriority() {
        MasterySnapshot highRisk = new MasterySnapshot(
                1L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(0.9), LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(1), "LEARNING", 2, 6);

        MasterySnapshot lowRisk = new MasterySnapshot(
                2L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(0.1), LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(1), "LEARNING", 2, 6);

        double highPriority = calculator.calculatePriority(highRisk);
        double lowPriority = calculator.calculatePriority(lowRisk);

        assertThat(highPriority).isGreaterThan(lowPriority);
    }

    @Test
    void recentExposureReducesPriority() {
        MasterySnapshot recent = new MasterySnapshot(
                1L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(0.5), LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusHours(1), "LEARNING", 3, 3);

        MasterySnapshot old = new MasterySnapshot(
                2L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.3),
                BigDecimal.valueOf(0.5), LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusHours(1), "LEARNING", 3, 3);

        double recentPriority = calculator.calculatePriority(recent);
        double oldPriority = calculator.calculatePriority(old);

        assertThat(oldPriority).isGreaterThan(recentPriority);
    }

    @Test
    void outputGapIncreasesPriority() {
        MasterySnapshot bigGap = new MasterySnapshot(
                1L, BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.2),
                BigDecimal.valueOf(0.5), LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(1), "LEARNING", 3, 3);

        MasterySnapshot noGap = new MasterySnapshot(
                2L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(0.5),
                BigDecimal.valueOf(0.5), LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(1), "LEARNING", 3, 3);

        double gapPriority = calculator.calculatePriority(bigGap);
        double noGapPriority = calculator.calculatePriority(noGap);

        assertThat(gapPriority).isGreaterThan(noGapPriority);
    }

    @Test
    void priorityIsClampedBetweenZeroAndOne() {
        MasterySnapshot extreme = new MasterySnapshot(
                1L, BigDecimal.valueOf(1.0), BigDecimal.valueOf(0.0),
                BigDecimal.valueOf(1.0), LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(28), "LEARNING", 0, 100);

        double priority = calculator.calculatePriority(extreme);
        assertThat(priority).isBetween(0.0, 1.0);
    }
}
