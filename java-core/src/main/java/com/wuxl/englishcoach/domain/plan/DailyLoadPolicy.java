package com.wuxl.englishcoach.domain.plan;

import org.springframework.stereotype.Component;

@Component
public class DailyLoadPolicy {

    public record LoadResult(int newCount, int reviewCount, int outputCount, String loadDecision, String loadReason) {}

    public LoadResult calculate(int dailyMinutes, double recentCompletionRate, double recentAccuracy,
                                double fatigueRatio, long overdueCount) {
        int[] base = baseLoad(dailyMinutes);
        int newCount = base[0];
        int reviewCount = base[1];
        int outputCount = base[2];

        StringBuilder reason = new StringBuilder("base tier " + dailyMinutes + "min");

        // high completion: +1 new, +1 review
        if (recentCompletionRate >= 0.85) {
            newCount += 1;
            reviewCount += 1;
            reason.append("; high completion +1n/+1r");
        }

        // high accuracy: +1 new
        if (recentAccuracy >= 0.85) {
            newCount += 1;
            reason.append("; high accuracy +1n");
        }

        // fatigue: -2 new, -1 output
        if (fatigueRatio >= 0.50) {
            newCount -= 2;
            outputCount = Math.max(0, outputCount - 1);
            reason.append("; fatigue -2n/-1o");
        }

        // low completion: -2 new
        if (recentCompletionRate < 0.50) {
            newCount -= 2;
            outputCount = Math.max(0, outputCount - 1);
            reason.append("; low completion -2n/-1o");
        }

        // many overdue: +3 review, -1 new
        if (overdueCount > 10) {
            reviewCount += 3;
            newCount = Math.max(1, newCount - 1);
            reason.append("; overdue +" + overdueCount + " +3r/-1n");
        }

        newCount = Math.max(1, newCount);
        reviewCount = Math.max(1, reviewCount);

        String decision = classifyLoad(dailyMinutes, newCount, reviewCount);

        return new LoadResult(newCount, reviewCount, outputCount, decision, reason.toString());
    }

    private int[] baseLoad(int dailyMinutes) {
        if (dailyMinutes <= 10) return new int[]{3, 6, 1};
        if (dailyMinutes <= 20) return new int[]{5, 10, 2};
        if (dailyMinutes <= 30) return new int[]{7, 12, 3};
        return new int[]{9, 15, 4};
    }

    private String classifyLoad(int dailyMinutes, int newCount, int reviewCount) {
        int[] base = baseLoad(dailyMinutes);
        int baseTotal = base[0] + base[1];
        int actualTotal = newCount + reviewCount;

        if (actualTotal <= baseTotal * 0.75) return "LIGHT";
        if (actualTotal >= baseTotal * 1.15) return "BOOST";
        return "NORMAL";
    }
}
