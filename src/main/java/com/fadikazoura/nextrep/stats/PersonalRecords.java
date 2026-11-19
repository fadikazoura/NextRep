package com.fadikazoura.nextrep.stats;

import java.math.BigDecimal;

/**
 * Best performances on one exercise. Any field is null when it does not apply
 * (e.g. no weighted sets for a bodyweight exercise).
 */
public record PersonalRecords(LoggedSet heaviestSet, LoggedSet bestEstimatedSet,
                              BigDecimal estimatedOneRepMaxKg, LoggedSet mostRepsSet) {
}
