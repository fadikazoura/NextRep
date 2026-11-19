package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.ExerciseResponse;
import com.fadikazoura.nextrep.exercise.MovementType;
import com.fadikazoura.nextrep.exercise.MuscleGroup;
import com.fadikazoura.nextrep.plan.PlanBlueprint.DayBlueprint;
import com.fadikazoura.nextrep.plan.PlanBlueprint.ExerciseSlot;
import com.fadikazoura.nextrep.user.Equipment;
import com.fadikazoura.nextrep.user.ExperienceLevel;
import com.fadikazoura.nextrep.user.TrainingGoal;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanBlueprintValidatorTest {

    private static final List<ExerciseResponse> CATALOG = List.of(
            new ExerciseResponse(1L, "Barbell Bench Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(2L, "Push-Up", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(3L, "Pull-Up", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(4L, "Bodyweight Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT));

    private static final PlanGenerationInput GYM = input(Equipment.FULL_GYM);
    private static final PlanGenerationInput HOME = input(Equipment.HOME);

    private final PlanBlueprintValidator validator = new PlanBlueprintValidator();

    @Test
    void acceptsAValidPlan() {
        assertThatCode(() -> validator.validate(validPlan(), GYM, CATALOG)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInventedExercise() {
        PlanBlueprint plan = planWithMondaySlot(new ExerciseSlot(999L, 3, 8, 12));

        assertThatThrownBy(() -> validator.validate(plan, GYM, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("Unknown exercise id 999");
    }

    @Test
    void rejectsExerciseThatNeedsMissingEquipment() {
        PlanBlueprint plan = planWithMondaySlot(new ExerciseSlot(1L, 3, 8, 12));

        assertThatThrownBy(() -> validator.validate(plan, HOME, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("Barbell Bench Press needs equipment");
    }

    @Test
    void rejectsPlanWhoseDaysDoNotMatchTrainingDays() {
        PlanBlueprint missingFriday = new PlanBlueprint("Plan", List.of(day(MONDAY), day(WEDNESDAY)));

        assertThatThrownBy(() -> validator.validate(missingFriday, GYM, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("do not match training days");
    }

    @Test
    void rejectsTheSameDayTwice() {
        PlanBlueprint plan = new PlanBlueprint("Plan", List.of(day(MONDAY), day(MONDAY), day(WEDNESDAY), day(FRIDAY)));

        assertThatThrownBy(() -> validator.validate(plan, GYM, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("more than once");
    }

    @Test
    void rejectsTheSameExerciseTwiceInADay() {
        PlanBlueprint plan = new PlanBlueprint("Plan", List.of(
                new DayBlueprint(MONDAY, "Full Body", List.of(new ExerciseSlot(2L, 3, 8, 12), new ExerciseSlot(2L, 3, 8, 12))),
                day(WEDNESDAY), day(FRIDAY)));

        assertThatThrownBy(() -> validator.validate(plan, GYM, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("appears twice");
    }

    @Test
    void rejectsImpossibleSetsAndReps() {
        assertThatThrownBy(() -> validator.validate(planWithMondaySlot(new ExerciseSlot(2L, 0, 8, 12)), GYM, CATALOG))
                .hasMessageContaining("sets must be between");
        assertThatThrownBy(() -> validator.validate(planWithMondaySlot(new ExerciseSlot(2L, 3, 12, 8)), GYM, CATALOG))
                .hasMessageContaining("invalid rep range");
        assertThatThrownBy(() -> validator.validate(planWithMondaySlot(new ExerciseSlot(2L, 3, 8, 80)), GYM, CATALOG))
                .hasMessageContaining("invalid rep range");
    }

    @Test
    void rejectsDayWithoutExercises() {
        PlanBlueprint plan = new PlanBlueprint("Plan", List.of(
                new DayBlueprint(MONDAY, "Rest?", List.of()), day(WEDNESDAY), day(FRIDAY)));

        assertThatThrownBy(() -> validator.validate(plan, GYM, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("has no exercises");
    }

    @Test
    void rejectsDayWithTooManyExercises() {
        List<ExerciseSlot> tooMany = new ArrayList<>();
        for (long id = 100; id < 111; id++) {
            tooMany.add(new ExerciseSlot(id, 3, 8, 12));
        }
        PlanBlueprint plan = new PlanBlueprint("Plan", List.of(
                new DayBlueprint(MONDAY, "Marathon", tooMany), day(WEDNESDAY), day(FRIDAY)));

        assertThatThrownBy(() -> validator.validate(plan, GYM, CATALOG))
                .hasMessageContaining("more than 10 exercises");
    }

    @Test
    void rejectsMissingNames() {
        PlanBlueprint noPlanName = new PlanBlueprint(" ", validPlan().days());

        assertThatThrownBy(() -> validator.validate(noPlanName, GYM, CATALOG))
                .hasMessageContaining("Plan name is missing");
    }

    private static PlanGenerationInput input(Equipment equipment) {
        return new PlanGenerationInput(Set.of(MONDAY, WEDNESDAY, FRIDAY), equipment,
                TrainingGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 60);
    }

    private static PlanBlueprint validPlan() {
        return new PlanBlueprint("Bodyweight Basics", List.of(day(MONDAY), day(WEDNESDAY), day(FRIDAY)));
    }

    private static PlanBlueprint planWithMondaySlot(ExerciseSlot slot) {
        return new PlanBlueprint("Plan", List.of(
                new DayBlueprint(MONDAY, "Full Body", List.of(slot)), day(WEDNESDAY), day(FRIDAY)));
    }

    private static DayBlueprint day(DayOfWeek dayOfWeek) {
        return new DayBlueprint(dayOfWeek, "Full Body", List.of(
                new ExerciseSlot(2L, 3, 8, 12),
                new ExerciseSlot(3L, 3, 6, 10),
                new ExerciseSlot(4L, 4, 12, 15)));
    }
}
