package com.wuxl.englishcoach.domain.memory;

import java.time.LocalDateTime;

public class MemoryPriorityPolicy {

    public double score(MemorySnapshot snapshot) {
        double score = Math.min(snapshot.seenCount(), 5) * 10;
        if (snapshot.nextDrillAt() != null && !snapshot.nextDrillAt().isAfter(LocalDateTime.now())) {
            score += 50;
        }
        if ("HIGH".equals(snapshot.severity())) {
            score += 20;
        }
        if ("IMPROVING".equals(snapshot.status())) {
            score -= 10;
        }
        return score;
    }

    public record MemorySnapshot(
            String memoryType,
            Long memoryId,
            String label,
            int seenCount,
            String severity,
            String status,
            LocalDateTime nextDrillAt
    ) {
    }
}
