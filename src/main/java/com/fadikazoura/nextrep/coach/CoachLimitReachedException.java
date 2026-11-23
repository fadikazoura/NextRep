package com.fadikazoura.nextrep.coach;

public class CoachLimitReachedException extends RuntimeException {

    public CoachLimitReachedException(int limit) {
        super("You've asked the coach " + limit + " questions today. Come back tomorrow for more.");
    }
}
