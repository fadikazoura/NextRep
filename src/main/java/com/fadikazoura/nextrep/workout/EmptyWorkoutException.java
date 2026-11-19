package com.fadikazoura.nextrep.workout;

public class EmptyWorkoutException extends RuntimeException {

    public EmptyWorkoutException(Long id) {
        super("Workout " + id + " has no sets; log at least one set or discard it");
    }
}
