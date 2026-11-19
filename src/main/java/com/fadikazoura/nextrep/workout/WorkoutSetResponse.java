package com.fadikazoura.nextrep.workout;

import java.math.BigDecimal;

public record WorkoutSetResponse(Long id, Long exerciseId, String exerciseName, int setNumber,
                                 BigDecimal weightKg, int reps) {

    public static WorkoutSetResponse from(WorkoutSet set) {
        return new WorkoutSetResponse(set.getId(), set.getExercise().getId(), set.getExercise().getName(),
                set.getSetNumber(), set.getWeightKg(), set.getReps());
    }
}
