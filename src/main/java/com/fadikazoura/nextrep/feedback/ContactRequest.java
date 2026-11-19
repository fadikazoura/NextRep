package com.fadikazoura.nextrep.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotNull ContactTopic topic,
        @NotBlank @Size(min = 10, max = 2000) String message
) {
}
