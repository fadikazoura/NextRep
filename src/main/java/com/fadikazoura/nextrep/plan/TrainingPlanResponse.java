package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.MuscleGroup;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record TrainingPlanResponse(Long id, String name, PlanSource source, Instant createdAt,
                                   List<DayResponse> days) {

    public record DayResponse(DayOfWeek dayOfWeek, String name, List<PlanExerciseResponse> exercises) {

        static DayResponse from(PlanDay day) {
            return new DayResponse(day.getDayOfWeek(), day.getName(),
                    day.getExercises().stream().map(PlanExerciseResponse::from).toList());
        }
    }

    public record PlanExerciseResponse(Long exerciseId, String name, MuscleGroup muscleGroup,
                                       int position, int sets, int repMin, int repMax) {

        static PlanExerciseResponse from(PlanExercise planExercise) {
            return new PlanExerciseResponse(planExercise.getExercise().getId(), planExercise.getExercise().getName(),
                    planExercise.getExercise().getMuscleGroup(), planExercise.getPosition(),
                    planExercise.getSets(), planExercise.getRepMin(), planExercise.getRepMax());
        }
    }

    public static TrainingPlanResponse from(TrainingPlan plan) {
        return new TrainingPlanResponse(plan.getId(), plan.getName(), plan.getSource(), plan.getCreatedAt(),
                plan.getDays().stream()
                        .sorted(Comparator.comparing(PlanDay::getDayOfWeek))
                        .map(DayResponse::from)
                        .toList());
    }
}
