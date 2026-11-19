package com.fadikazoura.nextrep.ai;

import com.fadikazoura.nextrep.exercise.ExerciseResponse;
import com.fadikazoura.nextrep.plan.InvalidPlanException;
import com.fadikazoura.nextrep.plan.PlanBlueprint;
import com.fadikazoura.nextrep.plan.PlanBlueprintValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Asks the AI for a plan, then parses and validates the answer before anyone uses it. */
@Component
@RequiredArgsConstructor
public class AiPlanGenerator {

    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final GeminiClient geminiClient;
    private final PlanBlueprintValidator validator;

    public boolean isEnabled() {
        return geminiClient.isEnabled();
    }

    /**
     * @throws AiUnavailableException if the AI cannot be reached or answers nothing
     * @throws InvalidPlanException   if the answer is not a valid plan for this athlete
     */
    public PlanBlueprint generate(AiPlanRequest request, List<ExerciseResponse> catalog) {
        String answer = geminiClient.generateJson(AiPlanPrompt.build(request, catalog), AiPlanPrompt.PLAN_SCHEMA);
        PlanBlueprint plan = parse(answer);
        validator.validate(plan, request.input(), catalog);
        return plan;
    }

    private static PlanBlueprint parse(String json) {
        try {
            return JSON.readValue(json, PlanBlueprint.class);
        } catch (JacksonException e) {
            throw new InvalidPlanException("AI answer is not a valid plan JSON: " + e.getOriginalMessage());
        }
    }
}
