package com.fadikazoura.nextrep.ai;

import com.fadikazoura.nextrep.exercise.ExerciseEquipment;
import com.fadikazoura.nextrep.exercise.ExerciseResponse;
import com.fadikazoura.nextrep.exercise.MovementType;
import com.fadikazoura.nextrep.exercise.MuscleGroup;
import com.fadikazoura.nextrep.plan.InvalidPlanException;
import com.fadikazoura.nextrep.plan.PlanBlueprint;
import com.fadikazoura.nextrep.plan.PlanBlueprintValidator;
import com.fadikazoura.nextrep.plan.PlanGenerationInput;
import com.fadikazoura.nextrep.user.Equipment;
import com.fadikazoura.nextrep.user.ExperienceLevel;
import com.fadikazoura.nextrep.user.TrainingGoal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gemini is mocked: these tests check what we send and how we treat the answer. */
class AiPlanGeneratorTest {

    private static final List<ExerciseResponse> CATALOG = List.of(
            new ExerciseResponse(1L, "Barbell Bench Press", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BARBELL),
            new ExerciseResponse(2L, "Push-Up", MuscleGroup.CHEST, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(3L, "Pull-Up", MuscleGroup.BACK, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT),
            new ExerciseResponse(4L, "Bodyweight Squat", MuscleGroup.QUADS, MovementType.COMPOUND, ExerciseEquipment.BODYWEIGHT));

    private static final AiPlanRequest HOME_REQUEST = new AiPlanRequest(
            new PlanGenerationInput(Set.of(MONDAY, THURSDAY), Equipment.HOME, TrainingGoal.STRENGTH,
                    ExperienceLevel.BEGINNER, 45),
            24, "Mild shoulder pain");

    private static final String VALID_ANSWER = """
            {
              "name": "Home Strength",
              "days": [
                {"dayOfWeek": "MONDAY", "name": "Upper",
                 "exercises": [{"exerciseId": 2, "sets": 3, "repMin": 5, "repMax": 8},
                               {"exerciseId": 3, "sets": 3, "repMin": 4, "repMax": 6}]},
                {"dayOfWeek": "THURSDAY", "name": "Lower",
                 "exercises": [{"exerciseId": 4, "sets": 4, "repMin": 8, "repMax": 12}]}
              ]
            }
            """;

    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final AiPlanGenerator generator = new AiPlanGenerator(geminiClient, new PlanBlueprintValidator());

    @Test
    void returnsTheValidatedPlanFromTheAiAnswer() {
        when(geminiClient.generateJson(anyString(), any())).thenReturn(VALID_ANSWER);

        PlanBlueprint plan = generator.generate(HOME_REQUEST, CATALOG);

        assertThat(plan.name()).isEqualTo("Home Strength");
        assertThat(plan.days()).hasSize(2);
        assertThat(plan.days().getFirst().exercises().getFirst().exerciseId()).isEqualTo(2L);
    }

    @Test
    void sendsProfileInjuriesAndOnlyUsableExercisesWithTheSchema() {
        when(geminiClient.generateJson(anyString(), any())).thenReturn(VALID_ANSWER);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);

        generator.generate(HOME_REQUEST, CATALOG);

        verify(geminiClient).generateJson(prompt.capture(), eq(AiPlanPrompt.PLAN_SCHEMA));
        assertThat(prompt.getValue())
                .contains("Age: 24", "Goal: STRENGTH", "Training days: MONDAY, THURSDAY")
                .contains("<injuries>\nMild shoulder pain\n</injuries>")
                .contains("2 | Push-Up | CHEST | COMPOUND | BODYWEIGHT")
                .doesNotContain("Barbell Bench Press");
    }

    @Test
    void rejectsAnswerThatIsNotJson() {
        when(geminiClient.generateJson(anyString(), any())).thenReturn("Here is your plan: do push-ups!");

        assertThatThrownBy(() -> generator.generate(HOME_REQUEST, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("not a valid plan JSON");
    }

    @Test
    void rejectsHallucinatedExercise() {
        when(geminiClient.generateJson(anyString(), any()))
                .thenReturn(VALID_ANSWER.replace("\"exerciseId\": 4", "\"exerciseId\": 777"));

        assertThatThrownBy(() -> generator.generate(HOME_REQUEST, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("Unknown exercise id 777");
    }

    @Test
    void rejectsExerciseTheAthleteCannotDoAtHome() {
        when(geminiClient.generateJson(anyString(), any()))
                .thenReturn(VALID_ANSWER.replace("\"exerciseId\": 4", "\"exerciseId\": 1"));

        assertThatThrownBy(() -> generator.generate(HOME_REQUEST, CATALOG))
                .isInstanceOf(InvalidPlanException.class)
                .hasMessageContaining("needs equipment");
    }

    @Test
    void passesThroughWhenTheAiIsUnavailable() {
        when(geminiClient.generateJson(anyString(), any())).thenThrow(new AiUnavailableException("503"));

        assertThatThrownBy(() -> generator.generate(HOME_REQUEST, CATALOG))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void reportsNoneWhenThereAreNoInjuries() {
        AiPlanRequest healthy = new AiPlanRequest(HOME_REQUEST.input(), 30, null);

        assertThat(AiPlanPrompt.build(healthy, CATALOG)).contains("<injuries>\nNone reported\n</injuries>");
    }
}
