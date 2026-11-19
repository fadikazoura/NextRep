package com.fadikazoura.nextrep.workout;

public class WorkoutInProgressException extends RuntimeException {

    public WorkoutInProgressException(Long id) {
        super("Workout " + id + " is still in progress; complete it before starting a new one");
    }
}
