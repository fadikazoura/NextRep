package com.fadikazoura.nextrep.insights;

import com.fadikazoura.nextrep.insights.InsightsResponse.AchievementResponse;
import com.fadikazoura.nextrep.insights.InsightsResponse.WeekTotals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Streaks, weekly totals and achievements, computed from the completed workouts.
 * <p>
 * Nothing here is stored: it is all derived from the logged sets, so it can never drift out of sync with them
 * (delete a workout and the numbers follow). Pure logic with "today" passed in, so it is unit tested directly.
 */
final class InsightsCalculator {

    private InsightsCalculator() {
    }

    /** @param sessions completed workouts, oldest first */
    static InsightsResponse calculate(List<SessionTotals> sessions, int weeklyGoal, LocalDate today, DayOfWeek firstDay) {
        TreeMap<LocalDate, Week> weeks = new TreeMap<>();
        for (SessionTotals session : sessions) {
            weeks.computeIfAbsent(weekStart(session.date(), firstDay), Week::new).add(session);
        }

        LocalDate thisWeek = weekStart(today, firstDay);
        return new InsightsResponse(
                currentStreak(weeks, thisWeek),
                longestStreak(weeks),
                weeklyGoal,
                totals(weeks, thisWeek),
                totals(weeks, thisWeek.minusWeeks(1)),
                totals(weeks, thisWeek.minusWeeks(2)),
                achievements(sessions, weeklyGoal, firstDay));
    }

    static LocalDate weekStart(LocalDate date, DayOfWeek firstDay) {
        return date.with(TemporalAdjusters.previousOrSame(firstDay));
    }

    /** Counted back from this week; if this week has no workout yet it is skipped, not counted as a break. */
    static int currentStreak(TreeMap<LocalDate, Week> weeks, LocalDate thisWeek) {
        LocalDate week = weeks.containsKey(thisWeek) ? thisWeek : thisWeek.minusWeeks(1);
        int streak = 0;
        while (weeks.containsKey(week)) {
            streak++;
            week = week.minusWeeks(1);
        }
        return streak;
    }

    static int longestStreak(TreeMap<LocalDate, Week> weeks) {
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate week : weeks.keySet()) {
            run = previous != null && previous.plusWeeks(1).equals(week) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = week;
        }
        return longest;
    }

    private static WeekTotals totals(TreeMap<LocalDate, Week> weeks, LocalDate start) {
        Week week = weeks.getOrDefault(start, new Week(start));
        return new WeekTotals(start, week.workouts, week.sets, week.volumeKg);
    }

    /**
     * Replays the history in order, so each badge also knows the day it was earned:
     * the first workout after which its number reached the target.
     */
    private static List<AchievementResponse> achievements(List<SessionTotals> sessions, int weeklyGoal, DayOfWeek firstDay) {
        Map<Achievement.Metric, BigDecimal> value = new EnumMap<>(Achievement.Metric.class);
        for (Achievement.Metric metric : Achievement.Metric.values()) value.put(metric, BigDecimal.ZERO);
        Map<Achievement, LocalDate> unlockedOn = new EnumMap<>(Achievement.class);

        Map<LocalDate, Integer> workoutsPerWeek = new TreeMap<>();
        LocalDate runEnd = null;
        int run = 0;
        for (SessionTotals session : sessions) {
            LocalDate week = weekStart(session.date(), firstDay);
            int inWeek = workoutsPerWeek.merge(week, 1, Integer::sum);
            if (!week.equals(runEnd)) {
                run = runEnd != null && runEnd.plusWeeks(1).equals(week) ? run + 1 : 1;
                runEnd = week;
            }

            add(value, Achievement.Metric.WORKOUTS, BigDecimal.ONE);
            add(value, Achievement.Metric.SETS, BigDecimal.valueOf(session.sets()));
            if (weeklyGoal > 0 && inWeek == weeklyGoal) add(value, Achievement.Metric.PERFECT_WEEKS, BigDecimal.ONE);
            value.merge(Achievement.Metric.LONGEST_STREAK, BigDecimal.valueOf(run), BigDecimal::max);
            value.merge(Achievement.Metric.HEAVIEST_SET_KG, session.heaviestKg(), BigDecimal::max);
            value.merge(Achievement.Metric.BEST_SESSION_VOLUME_KG, session.volumeKg(), BigDecimal::max);

            for (Achievement achievement : Achievement.values()) {
                if (!unlockedOn.containsKey(achievement)
                        && value.get(achievement.metric).compareTo(BigDecimal.valueOf(achievement.target)) >= 0) {
                    unlockedOn.put(achievement, session.date());
                }
            }
        }

        List<AchievementResponse> result = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            BigDecimal current = value.get(achievement.metric).setScale(0, RoundingMode.DOWN);
            LocalDate on = unlockedOn.get(achievement);
            double progress = on != null ? 1
                    : Math.min(1, current.doubleValue() / achievement.target);
            result.add(new AchievementResponse(achievement, on != null, on, current, achievement.target,
                    Math.round(progress * 100) / 100.0));
        }
        return result;
    }

    private static void add(Map<Achievement.Metric, BigDecimal> value, Achievement.Metric metric, BigDecimal amount) {
        value.merge(metric, amount, BigDecimal::add);
    }

    /** Running totals of one calendar week. */
    static final class Week {
        final LocalDate start;
        int workouts;
        int sets;
        BigDecimal volumeKg = BigDecimal.ZERO;

        Week(LocalDate start) {
            this.start = start;
        }

        void add(SessionTotals session) {
            workouts++;
            sets += session.sets();
            volumeKg = volumeKg.add(session.volumeKg());
        }
    }
}
