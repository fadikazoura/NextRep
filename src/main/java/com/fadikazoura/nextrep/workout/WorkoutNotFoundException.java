package com.fadikazoura.nextrep.workout;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(Long id) {
        super("Workout " + id + " does not exist");
    }
}
