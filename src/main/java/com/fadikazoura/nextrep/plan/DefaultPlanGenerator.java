package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.ExerciseResponse;
import com.fadikazoura.nextrep.exercise.MovementType;
import com.fadikazoura.nextrep.exercise.MuscleGroup;
import com.fadikazoura.nextrep.plan.PlanBlueprint.DayBlueprint;
import com.fadikazoura.nextrep.plan.PlanBlueprint.ExerciseSlot;
import com.fadikazoura.nextrep.user.ExperienceLevel;
import com.fadikazoura.nextrep.user.TrainingGoal;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.fadikazoura.nextrep.exercise.MovementType.COMPOUND;
import static com.fadikazoura.nextrep.exercise.MovementType.ISOLATION;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.BACK;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.BICEPS;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.CALVES;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.CHEST;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.CORE;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.GLUTES;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.HAMSTRINGS;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.QUADS;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.SHOULDERS;
import static com.fadikazoura.nextrep.exercise.MuscleGroup.TRICEPS;

/**
 * Rule-based weekly plan generator. Deterministic: the same input always gives the same plan.
 * Also serves as the fallback when the AI planner fails.
 */
@Component
public class DefaultPlanGenerator {

    static final int MINUTES_PER_EXERCISE = 10;
    static final int MIN_EXERCISES_PER_DAY = 3;
    static final int MAX_EXERCISES_PER_DAY = 8;

    public PlanBlueprint generate(PlanGenerationInput input, List<ExerciseResponse> catalog) {
        if (input.trainingDays() == null || input.trainingDays().isEmpty()) {
            throw new IllegalArgumentException("At least one training day is required");
        }

        List<DayOfWeek> days = input.trainingDays().stream().sorted().toList();
        Split split = Split.forDayCount(days.size());
        Set<ExerciseEquipment> allowed = EquipmentAccess.allowedFor(input.equipment());
        List<ExerciseResponse> available = catalog.stream()
                .filter(exercise -> allowed.contains(exercise.equipment()))
                .toList();
        int exercisesPerDay = Math.clamp(input.sessionMinutes() / MINUTES_PER_EXERCISE,
                MIN_EXERCISES_PER_DAY, MAX_EXERCISES_PER_DAY);

        Map<DayTemplate, Integer> occurrences = new EnumMap<>(DayTemplate.class);
        List<DayBlueprint> planDays = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            DayTemplate template = split.templates.get(i % split.templates.size());
            int occurrence = occurrences.merge(template, 1, Integer::sum) - 1;
            planDays.add(buildDay(days.get(i), template, occurrence, available, exercisesPerDay, input));
        }
        return new PlanBlueprint(split.displayName + " - " + days.size() + " days", planDays);
    }

    private DayBlueprint buildDay(DayOfWeek dayOfWeek, DayTemplate template, int occurrence,
                                  List<ExerciseResponse> available, int limit, PlanGenerationInput input) {
        Set<Long> used = new HashSet<>();
        List<ExerciseSlot> slots = new ArrayList<>();
        for (Slot slot : template.slots) {
            if (slots.size() == limit) {
                break;
            }
            pickExercise(slot, occurrence, available, used).ifPresent(exercise -> {
                used.add(exercise.id());
                slots.add(toSlot(exercise, input));
            });
        }
        return new DayBlueprint(dayOfWeek, template.displayName, slots);
    }

    /**
     * Picks an exercise for the slot. The occurrence index rotates the choice, so a template that
     * repeats in the week (e.g. Push A / Push B) gets different exercises each time.
     */
    private Optional<ExerciseResponse> pickExercise(Slot slot, int occurrence,
                                                    List<ExerciseResponse> available, Set<Long> used) {
        List<ExerciseResponse> candidates = candidates(available, used, slot.muscleGroup(), slot.movementType());
        if (candidates.isEmpty()) {
            candidates = candidates(available, used, slot.muscleGroup(), null);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(candidates.get(occurrence % candidates.size()));
    }

    private List<ExerciseResponse> candidates(List<ExerciseResponse> available, Set<Long> used,
                                              MuscleGroup muscleGroup, MovementType movementType) {
        return available.stream()
                .filter(exercise -> exercise.muscleGroup() == muscleGroup)
                .filter(exercise -> movementType == null || exercise.movementType() == movementType)
                .filter(exercise -> !used.contains(exercise.id()))
                .sorted(Comparator.comparingInt(DefaultPlanGenerator::equipmentPreference)
                        .thenComparing(ExerciseResponse::name))
                .toList();
    }

    /** Barbells and cables first for compound lifts, cables and dumbbells first for isolation work. */
    private static int equipmentPreference(ExerciseResponse exercise) {
        List<ExerciseEquipment> order = exercise.movementType() == COMPOUND
                ? List.of(ExerciseEquipment.BARBELL, ExerciseEquipment.CABLE, ExerciseEquipment.DUMBBELL,
                        ExerciseEquipment.MACHINE, ExerciseEquipment.BODYWEIGHT)
                : List.of(ExerciseEquipment.CABLE, ExerciseEquipment.DUMBBELL, ExerciseEquipment.MACHINE,
                        ExerciseEquipment.BARBELL, ExerciseEquipment.BODYWEIGHT);
        return order.indexOf(exercise.equipment());
    }

    private static ExerciseSlot toSlot(ExerciseResponse exercise, PlanGenerationInput input) {
        boolean compound = exercise.movementType() == COMPOUND;
        int sets = compound && input.experienceLevel() != ExperienceLevel.BEGINNER ? 4 : 3;
        int[] reps = repRange(input.goal(), compound);
        return new ExerciseSlot(exercise.id(), sets, reps[0], reps[1]);
    }

    private static int[] repRange(TrainingGoal goal, boolean compound) {
        return switch (goal) {
            case STRENGTH -> compound ? new int[]{4, 6} : new int[]{8, 12};
            case HYPERTROPHY -> compound ? new int[]{8, 12} : new int[]{10, 15};
            case ENDURANCE -> compound ? new int[]{12, 20} : new int[]{15, 20};
        };
    }

    private record Slot(MuscleGroup muscleGroup, MovementType movementType) {
    }

    private enum Split {
        FULL_BODY("Full Body", List.of(DayTemplate.FULL_BODY)),
        UPPER_LOWER("Upper/Lower", List.of(DayTemplate.UPPER, DayTemplate.LOWER)),
        PUSH_PULL_LEGS("Push/Pull/Legs", List.of(DayTemplate.PUSH, DayTemplate.PULL, DayTemplate.LEGS));

        private final String displayName;
        private final List<DayTemplate> templates;

        Split(String displayName, List<DayTemplate> templates) {
            this.displayName = displayName;
            this.templates = templates;
        }

        static Split forDayCount(int days) {
            if (days <= 3) {
                return FULL_BODY;
            }
            return days == 4 ? UPPER_LOWER : PUSH_PULL_LEGS;
        }
    }

    /** Muscle-group slots in priority order; a short session keeps only the first ones. */
    private enum DayTemplate {
        FULL_BODY("Full Body", List.of(new Slot(QUADS, COMPOUND), new Slot(CHEST, COMPOUND),
                new Slot(BACK, COMPOUND), new Slot(HAMSTRINGS, COMPOUND), new Slot(SHOULDERS, COMPOUND),
                new Slot(BICEPS, ISOLATION), new Slot(TRICEPS, ISOLATION), new Slot(CORE, ISOLATION))),
        UPPER("Upper", List.of(new Slot(CHEST, COMPOUND), new Slot(BACK, COMPOUND),
                new Slot(SHOULDERS, COMPOUND), new Slot(BACK, COMPOUND), new Slot(CHEST, ISOLATION),
                new Slot(SHOULDERS, ISOLATION), new Slot(BICEPS, ISOLATION), new Slot(TRICEPS, ISOLATION))),
        LOWER("Lower", List.of(new Slot(QUADS, COMPOUND), new Slot(HAMSTRINGS, COMPOUND),
                new Slot(GLUTES, COMPOUND), new Slot(QUADS, COMPOUND), new Slot(HAMSTRINGS, ISOLATION),
                new Slot(QUADS, ISOLATION), new Slot(CALVES, ISOLATION), new Slot(CORE, ISOLATION))),
        PUSH("Push", List.of(new Slot(CHEST, COMPOUND), new Slot(SHOULDERS, COMPOUND),
                new Slot(CHEST, COMPOUND), new Slot(TRICEPS, COMPOUND), new Slot(SHOULDERS, ISOLATION),
                new Slot(CHEST, ISOLATION), new Slot(TRICEPS, ISOLATION), new Slot(CORE, ISOLATION))),
        PULL("Pull", List.of(new Slot(BACK, COMPOUND), new Slot(BACK, COMPOUND),
                new Slot(BACK, COMPOUND), new Slot(SHOULDERS, ISOLATION), new Slot(BACK, ISOLATION),
                new Slot(BICEPS, ISOLATION), new Slot(BICEPS, ISOLATION), new Slot(CORE, ISOLATION))),
        LEGS("Legs", List.of(new Slot(QUADS, COMPOUND), new Slot(HAMSTRINGS, COMPOUND),
                new Slot(QUADS, COMPOUND), new Slot(GLUTES, COMPOUND), new Slot(HAMSTRINGS, ISOLATION),
                new Slot(QUADS, ISOLATION), new Slot(CALVES, ISOLATION), new Slot(CORE, ISOLATION)));

        private final String displayName;
        private final List<Slot> slots;

        DayTemplate(String displayName, List<Slot> slots) {
            this.displayName = displayName;
            this.slots = slots;
        }
    }
}
