package com.fadikazoura.nextrep.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmation(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "The code has 6 digits") String code,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
