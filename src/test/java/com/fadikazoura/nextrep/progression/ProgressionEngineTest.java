package com.fadikazoura.nextrep.progression;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.MovementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressionEngineTest {

    /** Bench press style target: 4 sets × 8–12 reps. */
    private static final ExerciseTarget BENCH =
            new ExerciseTarget(1L, MovementType.COMPOUND, ExerciseEquipment.BARBELL, 4, 8, 12);
    /** Lateral raise style target: 3 sets × 10–15 reps. */
    private static final ExerciseTarget LATERAL_RAISE =
            new ExerciseTarget(2L, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL, 3, 10, 15);
    private static final ExerciseTarget PUSH_UP =
            new ExerciseTarget(3L, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT, 4, 8, 12);

    private final ProgressionEngine engine = new ProgressionEngine();

    @Test
    void startsWithoutWeightWhenThereIsNoHistory() {
        Recommendation recommendation = engine.recommend(BENCH, List.of());

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.START);
        assertThat(recommendation.weightKg()).isNull();
        assertThat(recommendation.sets()).isEqualTo(4);
        assertThat(recommendation.repMin()).isEqualTo(8);
        assertThat(recommendation.repMax()).isEqualTo(12);
    }

    @Test
    void addsTwoAndAHalfKilosToCompoundLiftAfterHittingTopOfRange() {
        Recommendation recommendation = engine.recommend(BENCH, history(session("80", 12, 12, 12, 12)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.INCREASE);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("82.5");
    }

    @Test
    void addsOneAndAQuarterKilosToIsolationExercise() {
        Recommendation recommendation = engine.recommend(LATERAL_RAISE, history(session("10", 15, 15, 16)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.INCREASE);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("11.25");
    }

    @Test
    void holdsWeightWhenAnySetMissedTheTarget() {
        Recommendation recommendation = engine.recommend(BENCH, history(session("80", 12, 12, 11, 10)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.HOLD);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("80");
    }

    @Test
    void holdsWeightWhenFewerSetsThanPlannedWereDone() {
        Recommendation recommendation = engine.recommend(BENCH, history(session("80", 12, 12, 12)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.HOLD);
    }

    @Test
    void extraBackOffSetDoesNotBlockProgress() {
        Recommendation recommendation = engine.recommend(BENCH, history(session("80", 12, 12, 12, 12, 7)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.INCREASE);
    }

    @Test
    void ignoresLighterWarmUpSets() {
        SessionPerformance withWarmUps = new SessionPerformance(Instant.now(), List.of(
                set("40", 10), set("60", 5),
                set("80", 12), set("80", 12), set("80", 12), set("80", 12)));

        Recommendation recommendation = engine.recommend(BENCH, history(withWarmUps));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.INCREASE);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("82.5");
    }

    @Test
    void deloadsAfterThreeStalledSessionsInARow() {
        Recommendation recommendation = engine.recommend(BENCH, history(
                session("80", 10, 9, 9, 8),
                session("80", 11, 10, 9, 8),
                session("80", 10, 10, 9, 9)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.DELOAD);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("72.5");
    }

    @Test
    void twoStalledSessionsAreNotEnoughForDeload() {
        Recommendation recommendation = engine.recommend(BENCH, history(
                session("80", 10, 9, 9, 8),
                session("80", 11, 10, 9, 8)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.HOLD);
    }

    @Test
    void onlyStallsAtTheCurrentWeightCount() {
        Recommendation recommendation = engine.recommend(BENCH, history(
                session("80", 10, 9, 9, 8),
                session("80", 11, 10, 9, 8),
                session("77.5", 10, 10, 9, 9)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.HOLD);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("80");
    }

    @Test
    void deloadRoundsToLoadableWeight() {
        Recommendation recommendation = engine.recommend(BENCH, history(
                session("57.5", 8, 8, 8, 8),
                session("57.5", 8, 8, 8, 8),
                session("57.5", 8, 8, 8, 8)));

        // 57.5 × 0.9 = 51.75 → nearest loadable weight (multiple of 1.25) is 51.25
        assertThat(recommendation.weightKg()).isEqualByComparingTo("51.25");
    }

    @Test
    void bodyweightExerciseKeepsZeroWeightAndAsksForMoreReps() {
        Recommendation recommendation = engine.recommend(PUSH_UP, history(session("0", 12, 12, 12, 12)));

        assertThat(recommendation.action()).isEqualTo(ProgressionAction.HOLD);
        assertThat(recommendation.weightKg()).isEqualByComparingTo("0");
        assertThat(recommendation.reason()).contains("more reps");
    }

    @Test
    void explainsTheDecision() {
        Recommendation recommendation = engine.recommend(BENCH, history(session("80", 12, 12, 12, 12)));

        assertThat(recommendation.reason()).isEqualTo("Hit 12 reps on all 4 sets at 80 kg last time");
    }

    private static SessionPerformance session(String weightKg, int... reps) {
        List<SetPerformance> sets = new ArrayList<>();
        for (int rep : reps) {
            sets.add(set(weightKg, rep));
        }
        return new SessionPerformance(Instant.now(), sets);
    }

    private static SetPerformance set(String weightKg, int reps) {
        return new SetPerformance(new BigDecimal(weightKg), reps);
    }

    /** Sessions given newest first, spaced a few days apart. */
    private static List<SessionPerformance> history(SessionPerformance... newestFirst) {
        List<SessionPerformance> sessions = new ArrayList<>();
        Instant date = Instant.now();
        for (SessionPerformance session : newestFirst) {
            sessions.add(new SessionPerformance(date, session.sets()));
            date = date.minus(3, ChronoUnit.DAYS);
        }
        return sessions;
    }
}
