package com.fadikazoura.nextrep.coach;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param history  the last turns of this conversation (the server keeps no chat history: the browser sends it)
 * @param language "en" or "he" — the coach answers in the app's language
 */
public record CoachRequest(
        @NotBlank @Size(max = 500) String question,
        @Size(max = 8) List<@Valid Turn> history,
        @Pattern(regexp = "en|he") String language) {

    public record Turn(@NotNull Boolean fromUser, @NotBlank @Size(max = 2000) String text) {
    }
}
