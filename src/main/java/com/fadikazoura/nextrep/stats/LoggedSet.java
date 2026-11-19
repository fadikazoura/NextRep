package com.fadikazoura.nextrep.stats;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One performed set of an exercise, as input for records and charts. */
public record LoggedSet(LocalDate date, BigDecimal weightKg, int reps) {
}
