package com.wuxl.englishcoach.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class EnumContractTest {

    @Test
    void shouldMatchDocumentedGoalTypes() {
        assertThat(Arrays.stream(GoalType.values()).map(Enum::name))
                .containsExactly("GENERAL", "EXAM", "WORK", "TRAVEL");
    }

    @Test
    void shouldMatchDocumentedMasteryStatuses() {
        assertThat(Arrays.stream(MasteryStatus.values()).map(Enum::name))
                .containsExactly("NEW", "LEARNING", "REVIEWING", "WEAK_OUTPUT", "MASTERED", "ARCHIVED");
    }

    @Test
    void shouldMatchCoreAttemptAndPlanEnums() {
        assertThat(Arrays.stream(AttemptResult.values()).map(Enum::name))
                .containsExactly("CORRECT", "WRONG", "PARTIAL", "SKIPPED");
        assertThat(Arrays.stream(PlanType.values()).map(Enum::name))
                .containsExactly("NORMAL", "LIGHT_REVIEW");
    }

    @Test
    void shouldExposeStableErrorCodeMapping() {
        assertThat(ErrorCodeEnum.INVALID_REQUEST.getCode()).isEqualTo(4001);
        assertThat(ErrorCodeEnum.USER_NOT_FOUND.getCode()).isEqualTo(4041);
        assertThat(ErrorCodeEnum.INTERNAL_SERVER_ERROR.getCode()).isEqualTo(5000);
    }
}
