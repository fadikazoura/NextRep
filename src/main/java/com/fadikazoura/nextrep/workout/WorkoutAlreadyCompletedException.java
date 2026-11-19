package com.fadikazoura.nextrep.workout;

public class WorkoutAlreadyCompletedException extends RuntimeException {

    public WorkoutAlreadyCompletedException(Long id) {
        super("Workout " + id + " is already completed and can no longer be changed");
    }
}
