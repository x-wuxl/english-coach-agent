package com.wuxl.englishcoach.domain.plan;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NewItemSelectionPolicy {

    public record ItemCandidate(Long id, String itemType, String theme, int difficulty) {}

    public List<ItemCandidate> select(List<ItemCandidate> allCandidates, Set<Long> alreadyMasteredIds,
                                       List<String> preferredThemes, int desiredCount) {
        return allCandidates.stream()
                .filter(c -> !alreadyMasteredIds.contains(c.id()))
                .sorted(Comparator
                        .comparingInt((ItemCandidate c) -> themeMatchScore(c.theme(), preferredThemes)).reversed()
                        .thenComparingInt(ItemCandidate::difficulty))
                .limit(desiredCount)
                .collect(Collectors.toList());
    }

    private int themeMatchScore(String theme, List<String> preferredThemes) {
        if (preferredThemes == null || preferredThemes.isEmpty()) return 0;
        return preferredThemes.contains(theme) ? 1 : 0;
    }
}
