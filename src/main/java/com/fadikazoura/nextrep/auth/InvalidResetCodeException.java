package com.fadikazoura.nextrep.auth;

/** Same message for every failure (wrong, expired, used, unknown email) so nothing is revealed. */
public class InvalidResetCodeException extends RuntimeException {

    public InvalidResetCodeException() {
        super("The code is wrong or has expired. Request a new one.");
    }
}
