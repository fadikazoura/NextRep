package com.fadikazoura.nextrep.user;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email " + email + " is already registered");
    }
}
