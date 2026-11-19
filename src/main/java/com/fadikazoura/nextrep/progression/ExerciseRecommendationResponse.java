package com.fadikazoura.nextrep.progression;

import java.math.BigDecimal;

public record ExerciseRecommendationResponse(Long exerciseId, String exerciseName, ProgressionAction action,
                                             BigDecimal weightKg, int sets, int repMin, int repMax,
                                             String reason) {

    static ExerciseRecommendationResponse from(String exerciseName, Recommendation recommendation) {
        return new ExerciseRecommendationResponse(recommendation.exerciseId(), exerciseName,
                recommendation.action(), recommendation.weightKg(), recommendation.sets(),
                recommendation.repMin(), recommendation.repMax(), recommendation.reason());
    }
}
