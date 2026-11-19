package com.fadikazoura.nextrep.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/** {@code login} is the email or the username; the old field name "email" still works. */
public record LoginRequest(
        @NotBlank
        @JsonAlias("email")
        String login,

        @NotBlank
        String password
) {
}
