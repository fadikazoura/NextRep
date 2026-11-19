package com.fadikazoura.nextrep.exercise;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException(Long id) {
        super("Exercise " + id + " does not exist");
    }
}
