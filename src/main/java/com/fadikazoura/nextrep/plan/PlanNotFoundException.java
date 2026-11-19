package com.fadikazoura.nextrep.plan;

public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException() {
        super("No active training plan yet");
    }
}
