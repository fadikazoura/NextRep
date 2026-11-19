package com.fadikazoura.nextrep.account;

import jakarta.validation.constraints.NotBlank;

public record AvatarPresetRequest(@NotBlank String preset) {
}
