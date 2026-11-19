package com.fadikazoura.nextrep.stats;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Pure calculations for personal records and progress charts. */
@Component
public class PersonalRecordCalculator {

    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);

    /** On ties, the earlier set wins: that is when the record was actually set. */
    private static final Comparator<LoggedSet> EARLIER_FIRST =
            Comparator.comparing(LoggedSet::date, Comparator.reverseOrder());

    /**
     * Epley formula: 1RM = weight × (1 + reps / 30). A single rep is already a 1RM.
     * Computed as weight × (30 + reps) / 30 to divide only once.
     */
    public static BigDecimal estimateOneRepMax(BigDecimal weightKg, int reps) {
        if (reps <= 0 || weightKg.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (reps == 1) {
            return weightKg.setScale(2, RoundingMode.HALF_UP);
        }
        return weightKg.multiply(BigDecimal.valueOf(30L + reps)).divide(THIRTY, 2, RoundingMode.HALF_UP);
    }

    public PersonalRecords records(List<LoggedSet> sets) {
        LoggedSet heaviest = sets.stream()
                .filter(set -> set.weightKg().signum() > 0 && set.reps() > 0)
                .max(Comparator.comparing(LoggedSet::weightKg)
                        .thenComparingInt(LoggedSet::reps)
                        .thenComparing(EARLIER_FIRST))
                .orElse(null);

        LoggedSet bestEstimated = sets.stream()
                .filter(set -> set.weightKg().signum() > 0 && set.reps() > 0)
                .max(Comparator.comparing((LoggedSet set) -> estimateOneRepMax(set.weightKg(), set.reps()))
                        .thenComparing(EARLIER_FIRST))
                .orElse(null);

        LoggedSet mostReps = sets.stream()
                .filter(set -> set.reps() > 0)
                .max(Comparator.comparingInt(LoggedSet::reps)
                        .thenComparing(LoggedSet::weightKg)
                        .thenComparing(EARLIER_FIRST))
                .orElse(null);

        BigDecimal estimate = bestEstimated == null ? null
                : estimateOneRepMax(bestEstimated.weightKg(), bestEstimated.reps());
        return new PersonalRecords(heaviest, bestEstimated, estimate, mostReps);
    }

    /** One point per training day, oldest first. */
    public List<ProgressPoint> progress(List<LoggedSet> sets) {
        Map<LocalDate, List<LoggedSet>> byDate = sets.stream()
                .collect(Collectors.groupingBy(LoggedSet::date, TreeMap::new, Collectors.toList()));

        return byDate.entrySet().stream()
                .map(entry -> toPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static ProgressPoint toPoint(LocalDate date, List<LoggedSet> sets) {
        BigDecimal topWeight = sets.stream()
                .map(LoggedSet::weightKg)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal bestEstimate = sets.stream()
                .map(set -> estimateOneRepMax(set.weightKg(), set.reps()))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal volume = sets.stream()
                .map(set -> set.weightKg().multiply(BigDecimal.valueOf(set.reps())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int maxReps = sets.stream().mapToInt(LoggedSet::reps).max().orElse(0);
        return new ProgressPoint(date, topWeight, bestEstimate, volume, maxReps);
    }
}
