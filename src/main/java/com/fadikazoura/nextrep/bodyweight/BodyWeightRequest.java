package com.fadikazoura.nextrep.bodyweight;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyWeightRequest(
        @NotNull @DecimalMin("25.0") @DecimalMax("400.0") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
        @PastOrPresent LocalDate recordedOn
) {
}
