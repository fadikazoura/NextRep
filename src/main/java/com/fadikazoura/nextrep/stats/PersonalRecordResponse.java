package com.fadikazoura.nextrep.stats;

import com.fadikazoura.nextrep.exercise.MuscleGroup;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PersonalRecordResponse(Long exerciseId, String exerciseName, MuscleGroup muscleGroup,
                                     RecordSet heaviest, RecordSet bestEstimated,
                                     BigDecimal estimatedOneRepMaxKg, RecordSet mostReps) {

    public record RecordSet(BigDecimal weightKg, int reps, LocalDate date) {

        static RecordSet from(LoggedSet set) {
            return set == null ? null : new RecordSet(set.weightKg(), set.reps(), set.date());
        }
    }
}
