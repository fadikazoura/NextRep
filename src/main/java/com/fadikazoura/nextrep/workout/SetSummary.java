package com.fadikazoura.nextrep.workout;

import java.math.BigDecimal;
import java.time.Instant;

/** Projection: one distinct (exercise, weight, reps) combination and when it was first done. */
public interface SetSummary {

    Long getExerciseId();

    BigDecimal getWeightKg();

    Integer getReps();

    Instant getFirstPerformedAt();
}
