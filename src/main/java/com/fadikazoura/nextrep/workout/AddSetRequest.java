package com.fadikazoura.nextrep.workout;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddSetRequest(
        @NotNull Long exerciseId,
        @NotNull @DecimalMin("0.0") @DecimalMax("1000.0") @Digits(integer = 4, fraction = 2) BigDecimal weightKg,
        @NotNull @Min(0) @Max(100) Integer reps
) {
}
