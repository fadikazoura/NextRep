package com.fadikazoura.nextrep.insights;

import com.fadikazoura.nextrep.insights.InsightsResponse.AchievementResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsightsCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25); // a Wednesday

    @Test
    void noWorkoutsMeansNoStreakAndEverythingLocked() {
        InsightsResponse insights = calculate(List.of(), 3);

        assertThat(insights.streakWeeks()).isZero();
        assertThat(insights.thisWeek().workouts()).isZero();
        assertThat(insights.achievements()).noneMatch(AchievementResponse::unlocked);
    }

    @Test
    void streakCountsConsecutiveWeeksAndAnEmptyCurrentWeekDoesNotBreakIt() {
        // last week, 2 and 3 weeks ago — nothing yet this week; 5 weeks ago is after a gap
        InsightsResponse insights = calculate(List.of(
                session(TODAY.minusWeeks(5)),
                session(TODAY.minusWeeks(3)), session(TODAY.minusWeeks(2)), session(TODAY.minusWeeks(1))), 3);

        assertThat(insights.streakWeeks()).isEqualTo(3);
        assertThat(insights.longestStreakWeeks()).isEqualTo(3);
    }

    @Test
    void aMissedWeekEndsTheStreak() {
        InsightsResponse insights = calculate(List.of(session(TODAY.minusWeeks(3)), session(TODAY.minusWeeks(2))), 3);

        assertThat(insights.streakWeeks()).isZero(); // last week was empty
        assertThat(insights.longestStreakWeeks()).isEqualTo(2);
    }

    @Test
    void theFirstDayOfTheWeekDecidesWhereASundayWorkoutBelongs() {
        LocalDate sunday = LocalDate.of(2026, 11, 22);
        List<SessionTotals> sessions = List.of(session(sunday));

        InsightsResponse mondayWeeks = InsightsCalculator.calculate(sessions, 3, TODAY, DayOfWeek.MONDAY);
        InsightsResponse sundayWeeks = InsightsCalculator.calculate(sessions, 3, TODAY, DayOfWeek.SUNDAY);

        assertThat(mondayWeeks.lastWeek().workouts()).isEqualTo(1);
        assertThat(sundayWeeks.thisWeek().workouts()).isEqualTo(1);
    }

    @Test
    void weekTotalsAddUpSetsAndVolume() {
        InsightsResponse insights = calculate(List.of(
                new SessionTotals(TODAY.minusDays(1), 12, new BigDecimal("5000"), new BigDecimal("80")),
                new SessionTotals(TODAY, 10, new BigDecimal("4000"), new BigDecimal("90"))), 3);

        assertThat(insights.thisWeek().workouts()).isEqualTo(2);
        assertThat(insights.thisWeek().sets()).isEqualTo(22);
        assertThat(insights.thisWeek().volumeKg()).isEqualByComparingTo("9000");
    }

    @Test
    void badgesRememberTheDayTheyWereEarned() {
        List<SessionTotals> sessions = new ArrayList<>();
        for (int i = 0; i < 10; i++) sessions.add(session(TODAY.minusDays(30 - i * 3L)));

        InsightsResponse insights = calculate(sessions, 3);

        AchievementResponse first = badge(insights, Achievement.FIRST_WORKOUT);
        AchievementResponse ten = badge(insights, Achievement.WORKOUTS_10);
        AchievementResponse twentyFive = badge(insights, Achievement.WORKOUTS_25);
        assertThat(first.unlockedOn()).isEqualTo(sessions.getFirst().date());
        assertThat(ten.unlockedOn()).isEqualTo(sessions.getLast().date());
        assertThat(twentyFive.unlocked()).isFalse();
        assertThat(twentyFive.progress()).isEqualTo(0.4);
    }

    @Test
    void perfectWeekNeedsTheWeeklyGoal() {
        LocalDate monday = LocalDate.of(2026, 11, 16);
        List<SessionTotals> twoWorkouts = List.of(session(monday), session(monday.plusDays(2)));

        assertThat(badge(calculate(twoWorkouts, 3), Achievement.PERFECT_WEEK).unlocked()).isFalse();
        assertThat(badge(calculate(twoWorkouts, 2), Achievement.PERFECT_WEEK).unlocked()).isTrue();
    }

    @Test
    void heavyLiftsAndBigSessionsUnlockTheirBadges() {
        InsightsResponse insights = calculate(List.of(
                new SessionTotals(TODAY.minusDays(2), 20, new BigDecimal("10500"), new BigDecimal("100"))), 3);

        assertThat(badge(insights, Achievement.CLUB_100).unlocked()).isTrue();
        assertThat(badge(insights, Achievement.BIG_SESSION).unlocked()).isTrue();
    }

    private static InsightsResponse calculate(List<SessionTotals> sessions, int goal) {
        return InsightsCalculator.calculate(sessions, goal, TODAY, DayOfWeek.MONDAY);
    }

    private static SessionTotals session(LocalDate date) {
        return new SessionTotals(date, 15, new BigDecimal("6000"), new BigDecimal("60"));
    }

    private static AchievementResponse badge(InsightsResponse insights, Achievement code) {
        return insights.achievements().stream().filter(a -> a.code() == code).findFirst().orElseThrow();
    }
}
