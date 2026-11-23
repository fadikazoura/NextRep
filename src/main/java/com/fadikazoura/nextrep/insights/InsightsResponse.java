package com.fadikazoura.nextrep.insights;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the home and progress pages show about consistency, in one call.
 *
 * @param streakWeeks consecutive weeks with at least one workout, up to this week (an empty current week
 *                    doesn't break the streak yet: there is still time)
 * @param weeklyGoal  workouts per week from the profile's training days (0 without a profile)
 */
public record InsightsResponse(int streakWeeks, int longestStreakWeeks, int weeklyGoal,
                               WeekTotals thisWeek, WeekTotals lastWeek, WeekTotals weekBefore,
                               List<AchievementResponse> achievements) {

    public record WeekTotals(LocalDate start, int workouts, int sets, BigDecimal volumeKg) {
    }

    /**
     * @param progress   0..1 towards the target (1 once unlocked)
     * @param unlockedOn the day the badge was earned, null while locked
     */
    public record AchievementResponse(Achievement code, boolean unlocked, LocalDate unlockedOn,
                                      BigDecimal current, int target, double progress) {
    }
}
