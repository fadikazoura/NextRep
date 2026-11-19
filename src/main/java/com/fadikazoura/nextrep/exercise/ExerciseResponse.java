package com.fadikazoura.nextrep.exercise;

public record ExerciseResponse(Long id, String name, MuscleGroup muscleGroup,
                               MovementType movementType, ExerciseEquipment equipment) {

    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
                exercise.getMovementType(), exercise.getEquipment());
    }
}
