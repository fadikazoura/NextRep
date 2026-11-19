package com.fadikazoura.nextrep.stats;

import com.fadikazoura.nextrep.exercise.MuscleGroup;

/** Completed sets per muscle group in a recent window; drives the muscle heat map. */
public record MuscleVolumeResponse(MuscleGroup muscleGroup, long sets) {
}
