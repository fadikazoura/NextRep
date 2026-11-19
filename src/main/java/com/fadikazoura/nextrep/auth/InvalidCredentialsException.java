package com.fadikazoura.nextrep.auth;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email/username or password");
    }
}
