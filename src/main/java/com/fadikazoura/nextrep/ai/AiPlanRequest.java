package com.fadikazoura.nextrep.ai;

import com.fadikazoura.nextrep.plan.PlanGenerationInput;

/** Everything the AI planner knows about the athlete. Injuries are free text written by the user. */
public record AiPlanRequest(PlanGenerationInput input, int age, String injuries) {
}
