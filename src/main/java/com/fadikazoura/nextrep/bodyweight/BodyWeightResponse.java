package com.fadikazoura.nextrep.bodyweight;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BodyWeightResponse(Long id, BigDecimal weightKg, LocalDate recordedOn) {

    public static BodyWeightResponse from(BodyWeightEntry entry) {
        return new BodyWeightResponse(entry.getId(), entry.getWeightKg(), entry.getRecordedOn());
    }
}
