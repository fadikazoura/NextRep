package com.fadikazoura.nextrep.progression;

import java.math.BigDecimal;

/** What to do next time on one exercise. {@code weightKg} is null when there is no history yet. */
public record Recommendation(Long exerciseId, ProgressionAction action, BigDecimal weightKg,
                             int sets, int repMin, int repMax, String reason) {
}
