package com.fadikazoura.nextrep.workout;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of the workout history: totals only, without the individual sets. */
public record WorkoutSummaryResponse(Long id, String planDayName, Instant startedAt, Instant completedAt,
                                     int setCount, int exerciseCount, BigDecimal totalVolumeKg) {

    public static WorkoutSummaryResponse from(WorkoutSession session) {
        BigDecimal volume = session.getSets().stream()
                .map(set -> set.getWeightKg().multiply(BigDecimal.valueOf(set.getReps())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int exerciseCount = (int) session.getSets().stream()
                .map(set -> set.getExercise().getId())
                .distinct()
                .count();

        return new WorkoutSummaryResponse(session.getId(),
                session.getPlanDay() != null ? session.getPlanDay().getName() : null,
                session.getStartedAt(), session.getCompletedAt(),
                session.getSets().size(), exerciseCount, volume);
    }
}
