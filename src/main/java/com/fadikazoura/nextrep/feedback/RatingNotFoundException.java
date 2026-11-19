package com.fadikazoura.nextrep.feedback;

public class RatingNotFoundException extends RuntimeException {

    public RatingNotFoundException() {
        super("You have not rated NextRep yet");
    }
}
