package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.ExerciseResponse;

import java.util.List;

/**
 * An optional, smarter plan generator (implemented by the AI planner).
 * Defined here so the plan module does not depend on any AI vendor (dependency inversion).
 * Any exception means "no smart plan this time": the caller falls back to the default generator.
 */
public interface SmartPlanGenerator {

    boolean isAvailable();

    PlanBlueprint generate(PlanGenerationInput input, int age, String injuries, List<ExerciseResponse> catalog);
}
