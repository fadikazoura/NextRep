package com.fadikazoura.nextrep.user;

public class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException() {
        super("This is not a valid phone number for the selected country");
    }
}
