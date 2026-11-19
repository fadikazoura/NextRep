package com.fadikazoura.nextrep.auth;

import com.fadikazoura.nextrep.user.Usernames;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The web app always sends a username; API clients may leave it out and get one made from their email.
 * The phone is optional and must include the country code, e.g. "+972501234567".
 */
public record RegisterRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Pattern(regexp = Usernames.PATTERN, message = Usernames.MESSAGE)
        String username,

        @Size(max = 30)
        String phone
) {

    public RegisterRequest(String email, String password) {
        this(email, password, null, null);
    }
}
