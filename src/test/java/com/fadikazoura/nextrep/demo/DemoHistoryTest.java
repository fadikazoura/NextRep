package com.fadikazoura.nextrep.demo;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.MovementType;
import com.fadikazoura.nextrep.exercise.MuscleGroup;
import com.fadikazoura.nextrep.plan.TrainingPlanResponse.PlanExerciseResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DemoHistoryTest {

    private static final DemoHistory.Exercise BENCH =
            new DemoHistory.Exercise(ExerciseEquipment.BARBELL, MovementType.COMPOUND, "Barbell Bench Press");
    private static final DemoHistory.Exercise PUSH_UP =
            new DemoHistory.Exercise(ExerciseEquipment.BODYWEIGHT, MovementType.COMPOUND, "Push-Up");
    private static final PlanExerciseResponse PLANNED =
            new PlanExerciseResponse(1L, "Barbell Bench Press", MuscleGroup.CHEST, 1, 3, 8, 12);

    @Test
    void repsClimbFirstThenTheWeight() {
        Random random = new Random(1);
        List<DemoHistory.Set> first = DemoHistory.setsFor(PLANNED, BENCH, 0, random);
        List<DemoHistory.Set> third = DemoHistory.setsFor(PLANNED, BENCH, 2, random);
        List<DemoHistory.Set> fourth = DemoHistory.setsFor(PLANNED, BENCH, 3, random);

        assertThat(first).hasSize(3);
        assertThat(first.getFirst().reps()).isEqualTo(8);
        assertThat(third.getFirst().reps()).isEqualTo(12);
        assertThat(third.getFirst().weightKg()).isEqualByComparingTo(first.getFirst().weightKg());
        assertThat(fourth.getFirst().weightKg()).isGreaterThan(third.getFirst().weightKg());
        assertThat(fourth.getFirst().reps()).isEqualTo(8);
    }

    @Test
    void bodyweightExercisesHaveNoLoad() {
        List<DemoHistory.Set> sets = DemoHistory.setsFor(PLANNED, PUSH_UP, 7, new Random(1));

        assertThat(sets).allSatisfy(set -> assertThat(set.weightKg()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void weightsRoundToRealPlates() {
        assertThat(DemoHistory.roundToPlates(61.4, ExerciseEquipment.BARBELL)).isEqualByComparingTo("62.5");
        assertThat(DemoHistory.roundToPlates(21.2, ExerciseEquipment.DUMBBELL)).isEqualByComparingTo("22");
    }

    @Test
    void bodyWeightCoversTheWholePeriodAndEndsToday() {
        LocalDate today = LocalDate.of(2026, 11, 23);
        List<DemoHistory.BodyWeight> entries = DemoHistory.bodyWeights(today, new Random(1));

        assertThat(entries.getFirst().date()).isAfterOrEqualTo(today.minusWeeks(DemoHistory.WEEKS)).isBefore(today.minusWeeks(7));
        assertThat(entries.getLast().date()).isEqualTo(today);
        assertThat(entries.getLast().weightKg()).isLessThan(entries.getFirst().weightKg());
    }
}
