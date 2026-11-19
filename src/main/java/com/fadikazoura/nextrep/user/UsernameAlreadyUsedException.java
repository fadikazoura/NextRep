package com.fadikazoura.nextrep.user;

public class UsernameAlreadyUsedException extends RuntimeException {

    public UsernameAlreadyUsedException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
