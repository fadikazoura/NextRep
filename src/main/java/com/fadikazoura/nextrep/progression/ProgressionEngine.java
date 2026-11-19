package com.fadikazoura.nextrep.progression;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.MovementType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Double progression: train in a rep range; once every planned set reaches the top of the range,
 * add weight. Three stalled sessions in a row at the same weight trigger a deload.
 * Pure and deterministic: no database, no clock.
 */
@Component
public class ProgressionEngine {

    static final BigDecimal COMPOUND_INCREMENT_KG = new BigDecimal("2.5");
    static final BigDecimal ISOLATION_INCREMENT_KG = new BigDecimal("1.25");
    static final BigDecimal DELOAD_FACTOR = new BigDecimal("0.90");
    static final BigDecimal PLATE_STEP_KG = new BigDecimal("1.25");
    static final int FAILURES_BEFORE_DELOAD = 3;

    /**
     * @param history completed sessions that included this exercise, newest first
     */
    public Recommendation recommend(ExerciseTarget target, List<SessionPerformance> history) {
        List<SessionPerformance> sessions = history.stream()
                .filter(session -> !session.sets().isEmpty())
                .toList();
        if (sessions.isEmpty()) {
            return recommendation(target, ProgressionAction.START, null,
                    "No history yet: pick a weight you can lift for %d clean reps".formatted(target.repMin()));
        }

        SessionPerformance last = sessions.getFirst();
        BigDecimal workingWeight = workingWeight(last);

        if (isBodyweightOnly(target, workingWeight)) {
            return recommendation(target, ProgressionAction.HOLD, workingWeight,
                    "Bodyweight exercise: aim for more reps than last time");
        }

        if (hitTopOfRange(last, workingWeight, target)) {
            BigDecimal next = workingWeight.add(increment(target));
            return recommendation(target, ProgressionAction.INCREASE, next,
                    "Hit %d reps on all %d sets at %s kg last time"
                            .formatted(target.repMax(), target.sets(), format(workingWeight)));
        }

        int stalls = consecutiveStallsAt(workingWeight, sessions, target);
        if (stalls >= FAILURES_BEFORE_DELOAD) {
            BigDecimal deloaded = roundToPlates(workingWeight.multiply(DELOAD_FACTOR));
            return recommendation(target, ProgressionAction.DELOAD, deloaded,
                    "Stalled %d sessions in a row at %s kg: drop 10%% and build back up"
                            .formatted(stalls, format(workingWeight)));
        }

        return recommendation(target, ProgressionAction.HOLD, workingWeight,
                "Repeat %s kg until all %d sets reach %d reps"
                        .formatted(format(workingWeight), target.sets(), target.repMax()));
    }

    /** The heaviest weight used in the session; lighter sets are treated as warm-ups. */
    private static BigDecimal workingWeight(SessionPerformance session) {
        return session.sets().stream()
                .map(SetPerformance::weightKg)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    /** Success = at least the planned number of sets at the working weight reached the top of the range. */
    private static boolean hitTopOfRange(SessionPerformance session, BigDecimal weight, ExerciseTarget target) {
        long successfulSets = session.sets().stream()
                .filter(set -> set.weightKg().compareTo(weight) == 0)
                .filter(set -> set.reps() >= target.repMax())
                .count();
        return successfulSets >= target.sets();
    }

    /** Counts the newest sessions in a row that stayed at this weight without reaching the target. */
    private static int consecutiveStallsAt(BigDecimal weight, List<SessionPerformance> sessions, ExerciseTarget target) {
        int stalls = 0;
        for (SessionPerformance session : sessions) {
            boolean sameWeight = workingWeight(session).compareTo(weight) == 0;
            if (!sameWeight || hitTopOfRange(session, weight, target)) {
                break;
            }
            stalls++;
        }
        return stalls;
    }

    private static boolean isBodyweightOnly(ExerciseTarget target, BigDecimal workingWeight) {
        return target.equipment() == ExerciseEquipment.BODYWEIGHT && workingWeight.signum() == 0;
    }

    private static BigDecimal increment(ExerciseTarget target) {
        return target.movementType() == MovementType.COMPOUND ? COMPOUND_INCREMENT_KG : ISOLATION_INCREMENT_KG;
    }

    /** Rounds to the nearest weight that can actually be loaded (multiples of 1.25 kg). */
    private static BigDecimal roundToPlates(BigDecimal weight) {
        return weight.divide(PLATE_STEP_KG, 0, RoundingMode.HALF_UP).multiply(PLATE_STEP_KG);
    }

    private static Recommendation recommendation(ExerciseTarget target, ProgressionAction action,
                                                 BigDecimal weight, String reason) {
        BigDecimal normalized = weight == null ? null : weight.setScale(2, RoundingMode.HALF_UP);
        return new Recommendation(target.exerciseId(), action, normalized,
                target.sets(), target.repMin(), target.repMax(), reason);
    }

    private static String format(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }
}
