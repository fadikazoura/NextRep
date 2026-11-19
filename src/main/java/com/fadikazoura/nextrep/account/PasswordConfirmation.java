package com.fadikazoura.nextrep.account;

import jakarta.validation.constraints.NotBlank;

/** Body of dangerous actions (deleting the account): the user types the password again. */
public record PasswordConfirmation(@NotBlank String password) {
}
