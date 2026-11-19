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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultPlanGeneratorTest {

    /** Mirrors the seed data in V4__seed_exercises.sql. */
    private static final List<ExerciseResponse> CATALOG = List.of(
            new ExerciseResponse(1L, "Barbell Bench Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(2L, "Incline Barbell Bench Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(3L, "Dumbbell Bench Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(4L, "Incline Dumbbell Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(5L, "Machine Chest Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.MACHINE),
            new ExerciseResponse(6L, "Push-Up", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(7L, "Dumbbell Fly", MuscleGroup.CHEST, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(8L, "Cable Crossover", MuscleGroup.CHEST, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(9L, "Pec Deck", MuscleGroup.CHEST, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(10L, "Deadlift", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(11L, "Barbell Row", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(12L, "Pull-Up", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(13L, "Chin-Up", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(14L, "Lat Pulldown", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.CABLE),
            new ExerciseResponse(15L, "Seated Cable Row", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.CABLE),
            new ExerciseResponse(16L, "One-Arm Dumbbell Row", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(17L, "Machine Row", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.MACHINE),
            new ExerciseResponse(18L, "Straight-Arm Pulldown", MuscleGroup.BACK, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(19L, "Overhead Press", MuscleGroup.SHOULDERS, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(20L, "Seated Dumbbell Shoulder Press", MuscleGroup.SHOULDERS, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(21L, "Machine Shoulder Press", MuscleGroup.SHOULDERS, MovementType.COMPOUND, ExerciseEquipment.MACHINE),
            new ExerciseResponse(22L, "Pike Push-Up", MuscleGroup.SHOULDERS, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(23L, "Dumbbell Lateral Raise", MuscleGroup.SHOULDERS, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(24L, "Cable Lateral Raise", MuscleGroup.SHOULDERS, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(25L, "Face Pull", MuscleGroup.SHOULDERS, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(26L, "Reverse Dumbbell Fly", MuscleGroup.SHOULDERS, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(27L, "Barbell Curl", MuscleGroup.BICEPS, MovementType.ISOLATION, ExerciseEquipment.BARBELL),
            new ExerciseResponse(28L, "Dumbbell Curl", MuscleGroup.BICEPS, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(29L, "Hammer Curl", MuscleGroup.BICEPS, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(30L, "Cable Curl", MuscleGroup.BICEPS, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(31L, "Machine Preacher Curl", MuscleGroup.BICEPS, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(32L, "Close-Grip Bench Press", MuscleGroup.TRICEPS, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(33L, "Dips", MuscleGroup.TRICEPS, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(34L, "Cable Triceps Pushdown", MuscleGroup.TRICEPS, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(35L, "Overhead Dumbbell Triceps Extension", MuscleGroup.TRICEPS, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(36L, "Skull Crusher", MuscleGroup.TRICEPS, MovementType.ISOLATION, ExerciseEquipment.BARBELL),
            new ExerciseResponse(37L, "Barbell Back Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(38L, "Front Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(39L, "Leg Press", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.MACHINE),
            new ExerciseResponse(40L, "Goblet Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(41L, "Bulgarian Split Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(42L, "Dumbbell Lunge", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(43L, "Bodyweight Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(44L, "Leg Extension", MuscleGroup.QUADS, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(45L, "Romanian Deadlift", MuscleGroup.HAMSTRINGS, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(46L, "Dumbbell Romanian Deadlift", MuscleGroup.HAMSTRINGS, MovementType.COMPOUND, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(47L, "Lying Leg Curl", MuscleGroup.HAMSTRINGS, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(48L, "Seated Leg Curl", MuscleGroup.HAMSTRINGS, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(49L, "Barbell Hip Thrust", MuscleGroup.GLUTES, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(50L, "Glute Bridge", MuscleGroup.GLUTES, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(51L, "Cable Glute Kickback", MuscleGroup.GLUTES, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(52L, "Standing Calf Raise", MuscleGroup.CALVES, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(53L, "Seated Calf Raise", MuscleGroup.CALVES, MovementType.ISOLATION, ExerciseEquipment.MACHINE),
            new ExerciseResponse(54L, "Dumbbell Calf Raise", MuscleGroup.CALVES, MovementType.ISOLATION, ExerciseEquipment.DUMBBELL),
            new ExerciseResponse(55L, "Hanging Leg Raise", MuscleGroup.CORE, MovementType.ISOLATION, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(56L, "Cable Crunch", MuscleGroup.CORE, MovementType.ISOLATION, ExerciseEquipment.CABLE),
            new ExerciseResponse(57L, "Ab Wheel Rollout", MuscleGroup.CORE, MovementType.ISOLATION, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(58L, "Crunch", MuscleGroup.CORE, MovementType.ISOLATION, ExerciseEquipment.BODYWEIGHT)
    );

    private static final Map<Long, ExerciseResponse> BY_ID = CATALOG.stream()
            .collect(Collectors.toMap(ExerciseResponse::id, Function.identity()));

    private final DefaultPlanGenerator generator = new DefaultPlanGenerator();

    @Test
    void upToThreeDaysGivesFullBodyOnEveryDayInWeekOrder() {
        PlanBlueprint plan = generate(Set.of(FRIDAY, MONDAY, WEDNESDAY), Equipment.FULL_GYM);

        assertThat(plan.name()).isEqualTo("Full Body - 3 days");
        assertThat(plan.days()).extracting(DayBlueprint::dayOfWeek).containsExactly(MONDAY, WEDNESDAY, FRIDAY);
        assertThat(plan.days()).extracting(DayBlueprint::name).containsOnly("Full Body");
    }

    @Test
    void fourDaysAlternatesUpperAndLower() {
        PlanBlueprint plan = generate(Set.of(MONDAY, TUESDAY, THURSDAY, FRIDAY), Equipment.FULL_GYM);

        assertThat(plan.days()).extracting(DayBlueprint::name).containsExactly("Upper", "Lower", "Upper", "Lower");
    }

    @Test
    void fiveDaysCyclesPushPullLegs() {
        PlanBlueprint plan = generate(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), Equipment.FULL_GYM);

        assertThat(plan.days()).extracting(DayBlueprint::name).containsExactly("Push", "Pull", "Legs", "Push", "Pull");
    }

    @Test
    void homeTrainingUsesOnlyBodyweightExercises() {
        PlanBlueprint plan = generate(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.HOME);

        assertThat(allExercises(plan)).isNotEmpty()
                .extracting(ExerciseResponse::equipment)
                .containsOnly(ExerciseEquipment.BODYWEIGHT);
    }

    @Test
    void dumbbellsOnlyNeverUsesBarbellsMachinesOrCables() {
        PlanBlueprint plan = generate(Set.of(MONDAY, TUESDAY, THURSDAY, FRIDAY, SATURDAY), Equipment.DUMBBELLS_ONLY);

        assertThat(allExercises(plan)).isNotEmpty()
                .extracting(ExerciseResponse::equipment)
                .containsOnly(ExerciseEquipment.DUMBBELL, ExerciseEquipment.BODYWEIGHT);
    }

    @Test
    void strengthGoalUsesLowRepsOnCompoundLifts() {
        PlanBlueprint plan = generator.generate(input(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM,
                TrainingGoal.STRENGTH, ExperienceLevel.INTERMEDIATE, 60), CATALOG);

        allSlots(plan).stream()
                .filter(slot -> BY_ID.get(slot.exerciseId()).movementType() == MovementType.COMPOUND)
                .forEach(slot -> {
                    assertThat(slot.repMin()).isEqualTo(4);
                    assertThat(slot.repMax()).isEqualTo(6);
                });
    }

    @Test
    void hypertrophyGoalUsesModerateReps() {
        PlanBlueprint plan = generate(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM);

        allSlots(plan).forEach(slot -> {
            boolean compound = BY_ID.get(slot.exerciseId()).movementType() == MovementType.COMPOUND;
            assertThat(slot.repMin()).isEqualTo(compound ? 8 : 10);
            assertThat(slot.repMax()).isEqualTo(compound ? 12 : 15);
        });
    }

    @Test
    void beginnersGetThreeSetsEverywhere() {
        PlanBlueprint plan = generator.generate(input(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM,
                TrainingGoal.HYPERTROPHY, ExperienceLevel.BEGINNER, 60), CATALOG);

        assertThat(allSlots(plan)).extracting(ExerciseSlot::sets).containsOnly(3);
    }

    @Test
    void experiencedLiftersGetFourSetsOnCompoundLifts() {
        PlanBlueprint plan = generate(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM);

        allSlots(plan).forEach(slot -> {
            boolean compound = BY_ID.get(slot.exerciseId()).movementType() == MovementType.COMPOUND;
            assertThat(slot.sets()).isEqualTo(compound ? 4 : 3);
        });
    }

    @Test
    void sessionLengthControlsExercisesPerDay() {
        PlanBlueprint shortSession = generator.generate(input(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM,
                TrainingGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 30), CATALOG);
        PlanBlueprint longSession = generator.generate(input(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM,
                TrainingGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 120), CATALOG);

        assertThat(shortSession.days()).allSatisfy(day -> assertThat(day.exercises()).hasSize(3));
        assertThat(longSession.days()).allSatisfy(day -> assertThat(day.exercises()).hasSize(8));
    }

    @Test
    void neverRepeatsAnExerciseWithinADay() {
        PlanBlueprint plan = generate(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY), Equipment.FULL_GYM);

        assertThat(plan.days()).allSatisfy(day ->
                assertThat(day.exercises()).extracting(ExerciseSlot::exerciseId).doesNotHaveDuplicates());
    }

    @Test
    void repeatedDayTypesGetDifferentExercises() {
        PlanBlueprint plan = generate(Set.of(MONDAY, WEDNESDAY, FRIDAY), Equipment.FULL_GYM);

        List<Long> monday = exerciseIds(plan.days().get(0));
        List<Long> wednesday = exerciseIds(plan.days().get(1));
        assertThat(monday).isNotEqualTo(wednesday);
        assertThat(monday.getFirst()).isNotEqualTo(wednesday.getFirst());
    }

    @Test
    void rejectsEmptyTrainingDays() {
        assertThatThrownBy(() -> generate(Set.of(), Equipment.FULL_GYM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PlanBlueprint generate(Set<DayOfWeek> days, Equipment equipment) {
        return generator.generate(input(days, equipment, TrainingGoal.HYPERTROPHY, ExperienceLevel.INTERMEDIATE, 60),
                CATALOG);
    }

    private static PlanGenerationInput input(Set<DayOfWeek> days, Equipment equipment, TrainingGoal goal,
                                             ExperienceLevel level, int minutes) {
        return new PlanGenerationInput(days, equipment, goal, level, minutes);
    }

    private static List<ExerciseSlot> allSlots(PlanBlueprint plan) {
        return plan.days().stream().flatMap(day -> day.exercises().stream()).toList();
    }

    private static List<ExerciseResponse> allExercises(PlanBlueprint plan) {
        return allSlots(plan).stream().map(slot -> BY_ID.get(slot.exerciseId())).toList();
    }

    private static List<Long> exerciseIds(DayBlueprint day) {
        return day.exercises().stream().map(ExerciseSlot::exerciseId).toList();
    }
}
