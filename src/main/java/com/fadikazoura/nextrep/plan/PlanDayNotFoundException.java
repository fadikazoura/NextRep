package com.fadikazoura.nextrep.plan;

public class PlanDayNotFoundException extends RuntimeException {

    public PlanDayNotFoundException(Long id) {
        super("Plan day " + id + " does not exist");
    }
}
