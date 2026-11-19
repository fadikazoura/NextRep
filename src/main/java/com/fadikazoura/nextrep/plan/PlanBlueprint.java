package com.fadikazoura.nextrep.plan;

import java.time.DayOfWeek;
import java.util.List;

/**
 * A plan described as plain data, before it is saved.
 * Produced by the default generator now and by the AI planner later, so both share one save path.
 */
public record PlanBlueprint(String name, List<DayBlueprint> days) {

    public record DayBlueprint(DayOfWeek dayOfWeek, String name, List<ExerciseSlot> exercises) {
    }

    public record ExerciseSlot(Long exerciseId, int sets, int repMin, int repMax) {
    }
}
