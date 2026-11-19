package com.fadikazoura.nextrep.account;

/** The "current password" typed to confirm a sensitive action was wrong. */
public class IncorrectPasswordException extends RuntimeException {

    private final String field;

    public IncorrectPasswordException(String field) {
        super("The password is incorrect");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
