package com.fadikazoura.nextrep.feedback;

public class TooManyMessagesException extends RuntimeException {

    public TooManyMessagesException() {
        super("You have sent several messages today. Please try again tomorrow.");
    }
}
