package com.wuxl.englishcoach.domain.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MemoryPriorityPolicyTest {

    private final MemoryPriorityPolicy policy = new MemoryPriorityPolicy();

    @Test
    void repeatedDueItemShouldRankAboveNewItem() {
        var repeated = new MemoryPriorityPolicy.MemorySnapshot(
                "ERROR_PATTERN", 1L, "need to + verb", 2, "MEDIUM", "ACTIVE",
                LocalDateTime.now().minusDays(1));
        var fresh = new MemoryPriorityPolicy.MemorySnapshot(
                "EXPRESSION_GAP", 2L, "我来不及了", 1, "MEDIUM", "ACTIVE",
                LocalDateTime.now().plusDays(3));

        assertThat(policy.score(repeated)).isGreaterThan(policy.score(fresh));
    }
}
