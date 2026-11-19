package com.fadikazoura.nextrep.progression;

import java.time.DayOfWeek;
import java.util.List;

public record PlanDayRecommendationsResponse(Long planDayId, DayOfWeek dayOfWeek, String dayName,
                                             List<ExerciseRecommendationResponse> exercises) {
}
