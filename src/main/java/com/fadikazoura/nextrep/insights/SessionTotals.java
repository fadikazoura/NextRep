package com.fadikazoura.nextrep.insights;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One completed workout, reduced to what the insights need; {@code date} is in the user's time zone. */
record SessionTotals(LocalDate date, int sets, BigDecimal volumeKg, BigDecimal heaviestKg) {
}
