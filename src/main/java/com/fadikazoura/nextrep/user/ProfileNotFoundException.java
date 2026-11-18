package com.fadikazoura.nextrep.user;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("Profile has not been created yet");
    }
}
