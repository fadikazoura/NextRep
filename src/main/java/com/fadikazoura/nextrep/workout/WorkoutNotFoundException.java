package com.fadikazoura.nextrep.workout;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(Long id) {
        super("Workout " + id + " does not exist");
    }

    private WorkoutNotFoundException(String message) {
        super(message);
    }

    public static WorkoutNotFoundException noneInProgress() {
        return new WorkoutNotFoundException("No workout in progress");
    }
}
