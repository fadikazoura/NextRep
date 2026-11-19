package com.fadikazoura.nextrep.plan;

/** A generated plan broke a rule (unknown exercise, wrong days, impossible rep range, ...). */
public class InvalidPlanException extends RuntimeException {

    public InvalidPlanException(String message) {
        super(message);
    }
}
