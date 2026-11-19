package com.fadikazoura.nextrep.workout;

import com.fadikazoura.nextrep.exercise.MuscleGroup;

/** Projection: how many sets the user did for one muscle group. */
public interface MuscleSetCount {

    MuscleGroup getMuscleGroup();

    long getSets();
}
