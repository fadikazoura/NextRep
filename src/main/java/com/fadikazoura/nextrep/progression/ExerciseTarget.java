package com.fadikazoura.nextrep.progression;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.MovementType;

/** What the plan asks for on one exercise: sets × a rep range. */
public record ExerciseTarget(Long exerciseId, MovementType movementType, ExerciseEquipment equipment,
                             int sets, int repMin, int repMax) {
}
