package com.fadikazoura.nextrep.stats;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One point on an exercise's progress chart: the best of that day. */
public record ProgressPoint(LocalDate date, BigDecimal topWeightKg, BigDecimal estimatedOneRepMaxKg,
                            BigDecimal volumeKg, int maxReps) {
}
