package com.fadikazoura.nextrep.progression;

import java.time.Instant;
import java.util.List;

/** All sets of one exercise in one completed workout. */
public record SessionPerformance(Instant performedAt, List<SetPerformance> sets) {
}
