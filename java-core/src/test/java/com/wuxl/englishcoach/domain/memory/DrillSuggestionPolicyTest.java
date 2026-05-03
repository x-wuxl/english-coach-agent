package com.wuxl.englishcoach.domain.memory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DrillSuggestionPolicyTest {

    private final DrillSuggestionPolicy policy = new DrillSuggestionPolicy();

    @Test
    void shouldSuggestDrillWhenSameErrorPatternAppearsTwice() {
        assertThat(policy.shouldSuggest("ERROR_PATTERN", 2, "ACTIVE")).isTrue();
    }

    @Test
    void shouldNotSuggestDrillForFirstOccurrence() {
        assertThat(policy.shouldSuggest("ERROR_PATTERN", 1, "ACTIVE")).isFalse();
    }
}
