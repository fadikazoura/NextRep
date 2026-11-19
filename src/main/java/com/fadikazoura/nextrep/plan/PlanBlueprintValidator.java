package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.ExerciseResponse;
import com.fadikazoura.nextrep.plan.PlanBlueprint.DayBlueprint;
import com.fadikazoura.nextrep.plan.PlanBlueprint.ExerciseSlot;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checks a plan before it is saved. Generated plans (especially from an AI) are treated like untrusted input:
 * every rule the database and the app rely on is verified here.
 */
@Component
public class PlanBlueprintValidator {

    static final int MAX_PLAN_NAME_LENGTH = 100;
    static final int MAX_DAY_NAME_LENGTH = 50;
    static final int MAX_EXERCISES_PER_DAY = 10;
    static final int MAX_SETS = 10;
    static final int MAX_REPS = 50;

    /**
     * @throws InvalidPlanException describing the first broken rule
     */
    public void validate(PlanBlueprint plan, PlanGenerationInput input, List<ExerciseResponse> catalog) {
        if (plan == null) {
            throw new InvalidPlanException("Plan is missing");
        }
        requireName(plan.name(), MAX_PLAN_NAME_LENGTH, "Plan name");
        if (plan.days() == null || plan.days().isEmpty()) {
            throw new InvalidPlanException("Plan has no days");
        }

        Set<DayOfWeek> seenDays = EnumSet.noneOf(DayOfWeek.class);
        for (DayBlueprint day : plan.days()) {
            if (day == null || day.dayOfWeek() == null) {
                throw new InvalidPlanException("A day is missing its day of week");
            }
            if (!seenDays.add(day.dayOfWeek())) {
                throw new InvalidPlanException(day.dayOfWeek() + " appears more than once");
            }
        }
        if (!seenDays.equals(EnumSet.copyOf(input.trainingDays()))) {
            throw new InvalidPlanException("Plan days " + seenDays + " do not match training days "
                    + input.trainingDays());
        }

        Set<ExerciseEquipment> allowedEquipment = EquipmentAccess.allowedFor(input.equipment());
        Map<Long, ExerciseResponse> exercisesById = catalog.stream()
                .collect(Collectors.toMap(ExerciseResponse::id, Function.identity()));
        for (DayBlueprint day : plan.days()) {
            validateDay(day, exercisesById, allowedEquipment);
        }
    }

    private static void validateDay(DayBlueprint day, Map<Long, ExerciseResponse> exercisesById,
                                    Set<ExerciseEquipment> allowedEquipment) {
        requireName(day.name(), MAX_DAY_NAME_LENGTH, day.dayOfWeek() + " name");
        List<ExerciseSlot> slots = day.exercises();
        if (slots == null || slots.isEmpty()) {
            throw new InvalidPlanException(day.dayOfWeek() + " has no exercises");
        }
        if (slots.size() > MAX_EXERCISES_PER_DAY) {
            throw new InvalidPlanException(day.dayOfWeek() + " has more than " + MAX_EXERCISES_PER_DAY + " exercises");
        }

        Set<Long> seenExercises = new HashSet<>();
        for (ExerciseSlot slot : slots) {
            if (slot == null || slot.exerciseId() == null) {
                throw new InvalidPlanException(day.dayOfWeek() + " has an exercise without id");
            }
            ExerciseResponse exercise = exercisesById.get(slot.exerciseId());
            if (exercise == null) {
                throw new InvalidPlanException("Unknown exercise id " + slot.exerciseId());
            }
            if (!allowedEquipment.contains(exercise.equipment())) {
                throw new InvalidPlanException(exercise.name() + " needs equipment the user does not have");
            }
            if (!seenExercises.add(slot.exerciseId())) {
                throw new InvalidPlanException(exercise.name() + " appears twice on " + day.dayOfWeek());
            }
            if (slot.sets() < 1 || slot.sets() > MAX_SETS) {
                throw new InvalidPlanException(exercise.name() + ": sets must be between 1 and " + MAX_SETS);
            }
            if (slot.repMin() < 1 || slot.repMax() > MAX_REPS || slot.repMin() > slot.repMax()) {
                throw new InvalidPlanException(exercise.name() + ": invalid rep range "
                        + slot.repMin() + "-" + slot.repMax());
            }
        }
    }

    private static void requireName(String name, int maxLength, String what) {
        if (name == null || name.isBlank()) {
            throw new InvalidPlanException(what + " is missing");
        }
        if (name.length() > maxLength) {
            throw new InvalidPlanException(what + " is longer than " + maxLength + " characters");
        }
    }
}
