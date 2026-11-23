package com.fadikazoura.nextrep.demo;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.MovementType;
import com.fadikazoura.nextrep.plan.TrainingPlanResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Generates a believable training history for a demo account: the plan's workouts on its training days for the
 * last few weeks, with weights that climb the way double progression would make them climb (reps go up first,
 * then the weight), an occasional missed session, and a body weight that slowly drifts down.
 * <p>
 * Pure logic (no database, no clock) so it can be unit tested; the caller decides "today" and the randomness.
 */
final class DemoHistory {

    static final int WEEKS = 8;
    private static final double SKIP_CHANCE = 0.12;

    record Exercise(ExerciseEquipment equipment, MovementType movementType, String name) {
    }

    record Set(long exerciseId, int setNumber, BigDecimal weightKg, int reps) {
    }

    record Workout(LocalDate date, long planDayId, List<Set> sets) {
    }

    record BodyWeight(LocalDate date, BigDecimal weightKg) {
    }

    private DemoHistory() {
    }

    /** Workouts on the plan's days from {@link #WEEKS} weeks ago up to yesterday, oldest first. */
    static List<Workout> workouts(TrainingPlanResponse plan, Map<Long, Exercise> catalog, LocalDate today,
                                  RandomGenerator random) {
        Map<Long, Integer> sessionsDone = new HashMap<>(); // per exercise: how many times it was trained so far
        List<Workout> workouts = new ArrayList<>();
        LocalDate thisWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);

        for (LocalDate date = today.minusWeeks(WEEKS); date.isBefore(today); date = date.plusDays(1)) {
            for (TrainingPlanResponse.DayResponse day : plan.days()) {
                if (day.dayOfWeek() != date.getDayOfWeek()) continue;
                // Skip a few sessions for realism, but never in the current week (it should look on track).
                if (date.isBefore(thisWeekStart) && random.nextDouble() < SKIP_CHANCE) continue;

                List<Set> sets = new ArrayList<>();
                for (TrainingPlanResponse.PlanExerciseResponse planned : day.exercises()) {
                    Exercise exercise = catalog.get(planned.exerciseId());
                    if (exercise == null) continue;
                    int session = sessionsDone.merge(planned.exerciseId(), 1, Integer::sum) - 1;
                    sets.addAll(setsFor(planned, exercise, session, random));
                }
                if (!sets.isEmpty()) workouts.add(new Workout(date, day.id(), sets));
            }
        }
        return workouts;
    }

    /**
     * Double progression, three sessions per step: the reps climb from the bottom of the range to the top,
     * then the weight goes up and the reps start low again.
     */
    static List<Set> setsFor(TrainingPlanResponse.PlanExerciseResponse planned, Exercise exercise, int session,
                             RandomGenerator random) {
        int step = session / 3;
        int stage = session % 3; // 0 = bottom of the rep range, 2 = top
        int reps = planned.repMin() + Math.round((planned.repMax() - planned.repMin()) * stage / 2f);
        BigDecimal weight;
        if (exercise.equipment() == ExerciseEquipment.BODYWEIGHT) {
            weight = BigDecimal.ZERO;
            reps = Math.min(planned.repMax() + 6, reps + step); // no load to add, so the reps keep growing
        } else {
            double kg = startingWeight(exercise) * (1 + 0.05 * step);
            weight = roundToPlates(kg, exercise.equipment());
        }

        List<Set> sets = new ArrayList<>();
        for (int number = 1; number <= planned.sets(); number++) {
            boolean last = number == planned.sets();
            // Fatigue: the last set sometimes falls a rep short.
            int done = last && random.nextDouble() < 0.4 ? Math.max(1, reps - 1) : reps;
            sets.add(new Set(planned.exerciseId(), number, weight, done));
        }
        return sets;
    }

    /** A reasonable intermediate starting weight, by equipment and movement. */
    static double startingWeight(Exercise exercise) {
        boolean compound = exercise.movementType() == MovementType.COMPOUND;
        double kg = switch (exercise.equipment()) {
            case BARBELL -> compound ? 60 : 25;
            case DUMBBELL -> compound ? 20 : 10;
            case MACHINE -> compound ? 50 : 30;
            case CABLE -> compound ? 35 : 15;
            case BODYWEIGHT -> 0;
        };
        String name = exercise.name().toLowerCase(Locale.ROOT);
        if (name.contains("deadlift")) kg *= 1.5;
        else if (name.contains("squat") || name.contains("leg press")) kg *= 1.3;
        else if (name.contains("overhead") || name.contains("shoulder press")) kg *= 0.7;
        return kg;
    }

    /** Barbells and machines move in 2.5 kg steps, dumbbells in 2 kg steps. */
    static BigDecimal roundToPlates(double kg, ExerciseEquipment equipment) {
        double step = equipment == ExerciseEquipment.DUMBBELL ? 2 : 2.5;
        return BigDecimal.valueOf(Math.round(kg / step) * step).setScale(2, RoundingMode.HALF_UP);
    }

    /** A weigh-in every three days, slowly going down with some day-to-day noise. */
    static List<BodyWeight> bodyWeights(LocalDate today, RandomGenerator random) {
        List<BodyWeight> entries = new ArrayList<>();
        int days = WEEKS * 7;
        for (int ago = 0; ago <= days; ago += 3) { // counted back from today, so the last weigh-in is today
            double kg = 82.4 - 0.045 * (days - ago) + (random.nextDouble() - 0.5) * 0.6;
            entries.addFirst(new BodyWeight(today.minusDays(ago), BigDecimal.valueOf(kg).setScale(1, RoundingMode.HALF_UP)));
        }
        return entries;
    }
}
