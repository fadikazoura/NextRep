package com.fadikazoura.nextrep.coach;

/** @param remainingToday questions left today (the AI quota is shared, so each user gets a daily allowance) */
public record CoachResponse(String answer, int remainingToday) {
}
