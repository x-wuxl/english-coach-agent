package com.wuxl.englishcoach.domain.memory;

public class DrillSuggestionPolicy {

    public boolean shouldSuggest(String memoryType, int seenCount, String status) {
        return "ACTIVE".equals(status) && seenCount >= 2;
    }
}
