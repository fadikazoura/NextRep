package com.fadikazoura.nextrep.account;

import com.fadikazoura.nextrep.user.Usernames;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The phone may be empty to remove it. The email changes through a verified flow, not here. */
public record AccountUpdateRequest(
        @NotBlank
        @Pattern(regexp = Usernames.PATTERN, message = Usernames.MESSAGE)
        String username,

        @Size(max = 30)
        String phone
) {
}
