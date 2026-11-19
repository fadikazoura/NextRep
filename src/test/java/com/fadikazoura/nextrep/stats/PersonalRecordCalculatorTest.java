package com.fadikazoura.nextrep.stats;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalRecordCalculatorTest {

    private static final LocalDate MON = LocalDate.of(2026, 11, 2);
    private static final LocalDate WED = LocalDate.of(2026, 11, 4);
    private static final LocalDate FRI = LocalDate.of(2026, 11, 6);

    private final PersonalRecordCalculator calculator = new PersonalRecordCalculator();

    @Test
    void epleyEstimatesOneRepMax() {
        assertThat(PersonalRecordCalculator.estimateOneRepMax(new BigDecimal("100"), 10)).isEqualByComparingTo("133.33");
        assertThat(PersonalRecordCalculator.estimateOneRepMax(new BigDecimal("80"), 5)).isEqualByComparingTo("93.33");
    }

    @Test
    void singleRepIsAlreadyTheOneRepMax() {
        assertThat(PersonalRecordCalculator.estimateOneRepMax(new BigDecimal("140"), 1)).isEqualByComparingTo("140");
    }

    @Test
    void zeroRepsOrZeroWeightEstimateZero() {
        assertThat(PersonalRecordCalculator.estimateOneRepMax(new BigDecimal("100"), 0)).isEqualByComparingTo("0");
        assertThat(PersonalRecordCalculator.estimateOneRepMax(BigDecimal.ZERO, 15)).isEqualByComparingTo("0");
    }

    @Test
    void heaviestSetAndBestEstimateCanComeFromDifferentSets() {
        PersonalRecords records = calculator.records(List.of(
                set(MON, "100", 10),   // 1RM ≈ 133.33
                set(WED, "120", 1)));  // heaviest, but 1RM = 120

        assertThat(records.heaviestSet()).isEqualTo(set(WED, "120", 1));
        assertThat(records.bestEstimatedSet()).isEqualTo(set(MON, "100", 10));
        assertThat(records.estimatedOneRepMaxKg()).isEqualByComparingTo("133.33");
    }

    @Test
    void moreRepsAtTheSameWeightIsTheHeavierRecord() {
        PersonalRecords records = calculator.records(List.of(
                set(MON, "100", 5),
                set(WED, "100", 8)));

        assertThat(records.heaviestSet()).isEqualTo(set(WED, "100", 8));
    }

    @Test
    void onTiesTheEarliestSetHoldsTheRecord() {
        PersonalRecords records = calculator.records(List.of(
                set(FRI, "100", 8),
                set(MON, "100", 8),
                set(WED, "100", 8)));

        assertThat(records.heaviestSet().date()).isEqualTo(MON);
        assertThat(records.bestEstimatedSet().date()).isEqualTo(MON);
        assertThat(records.mostRepsSet().date()).isEqualTo(MON);
    }

    @Test
    void bodyweightExerciseHasOnlyARepsRecord() {
        PersonalRecords records = calculator.records(List.of(
                set(MON, "0", 15),
                set(WED, "0", 18)));

        assertThat(records.heaviestSet()).isNull();
        assertThat(records.bestEstimatedSet()).isNull();
        assertThat(records.estimatedOneRepMaxKg()).isNull();
        assertThat(records.mostRepsSet()).isEqualTo(set(WED, "0", 18));
    }

    @Test
    void noSetsMeansNoRecords() {
        PersonalRecords records = calculator.records(List.of());

        assertThat(records.heaviestSet()).isNull();
        assertThat(records.mostRepsSet()).isNull();
    }

    @Test
    void progressHasOnePointPerDayOldestFirst() {
        List<ProgressPoint> points = calculator.progress(List.of(
                set(WED, "82.5", 8),
                set(MON, "80", 10),
                set(WED, "82.5", 7),
                set(MON, "80", 9)));

        assertThat(points).extracting(ProgressPoint::date).containsExactly(MON, WED);
    }

    @Test
    void progressPointSummarisesTheDay() {
        List<ProgressPoint> points = calculator.progress(List.of(
                set(MON, "60", 10),     // warm-up
                set(MON, "80", 10),
                set(MON, "80", 8)));

        ProgressPoint monday = points.getFirst();
        assertThat(monday.topWeightKg()).isEqualByComparingTo("80");
        assertThat(monday.estimatedOneRepMaxKg()).isEqualByComparingTo("106.67");   // 80 × 40 / 30
        assertThat(monday.volumeKg()).isEqualByComparingTo("2040");                 // 600 + 800 + 640
        assertThat(monday.maxReps()).isEqualTo(10);
    }

    private static LoggedSet set(LocalDate date, String weightKg, int reps) {
        return new LoggedSet(date, new BigDecimal(weightKg), reps);
    }
}
