package com.fadikazoura.nextrep.workout;

public class WorkoutSetNotFoundException extends RuntimeException {

    public WorkoutSetNotFoundException(Long id) {
        super("Set " + id + " does not exist in this workout");
    }
}
