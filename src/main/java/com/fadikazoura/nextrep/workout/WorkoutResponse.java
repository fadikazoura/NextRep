package com.fadikazoura.nextrep.workout;

import java.time.Instant;
import java.util.List;

public record WorkoutResponse(Long id, Long planDayId, String planDayName, Instant startedAt,
                              Instant completedAt, List<WorkoutSetResponse> sets) {

    public static WorkoutResponse from(WorkoutSession session) {
        return new WorkoutResponse(
                session.getId(),
                session.getPlanDay() != null ? session.getPlanDay().getId() : null,
                session.getPlanDay() != null ? session.getPlanDay().getName() : null,
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getSets().stream().map(WorkoutSetResponse::from).toList());
    }
}
