package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.Exercise;
import com.fadikazoura.nextrep.exercise.ExerciseRepository;
import com.fadikazoura.nextrep.exercise.ExerciseResponse;
import com.fadikazoura.nextrep.plan.PlanBlueprint.DayBlueprint;
import com.fadikazoura.nextrep.plan.PlanBlueprint.ExerciseSlot;
import com.fadikazoura.nextrep.user.ProfileNotFoundException;
import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserProfile;
import com.fadikazoura.nextrep.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final UserProfileRepository userProfileRepository;
    private final ExerciseRepository exerciseRepository;
    private final DefaultPlanGenerator defaultPlanGenerator;

    @Transactional
    public TrainingPlanResponse generateDefaultPlan(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(ProfileNotFoundException::new);
        List<ExerciseResponse> catalog = exerciseRepository.findAll().stream()
                .map(ExerciseResponse::from)
                .toList();

        PlanBlueprint blueprint = defaultPlanGenerator.generate(PlanGenerationInput.from(profile), catalog);
        return TrainingPlanResponse.from(savePlan(profile.getUser(), blueprint, PlanSource.DEFAULT));
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse getActivePlan(Long userId) {
        return trainingPlanRepository.findByUserIdAndActiveTrue(userId)
                .map(TrainingPlanResponse::from)
                .orElseThrow(PlanNotFoundException::new);
    }

    /**
     * Saves a blueprint as the user's new active plan. Shared by the default generator and, later, the AI planner.
     */
    TrainingPlan savePlan(User user, PlanBlueprint blueprint, PlanSource source) {
        trainingPlanRepository.findByUserIdAndActiveTrue(user.getId()).ifPresent(current -> {
            current.deactivate();
            // Hibernate runs INSERTs before UPDATEs. Without this flush the new active plan would be
            // inserted while the old one is still active, breaking the one-active-plan unique index.
            trainingPlanRepository.saveAndFlush(current);
        });

        Map<Long, Exercise> exercises = exerciseRepository.findAllById(blueprint.days().stream()
                        .flatMap(day -> day.exercises().stream())
                        .map(ExerciseSlot::exerciseId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));

        TrainingPlan plan = new TrainingPlan(user, blueprint.name(), source);
        for (DayBlueprint dayBlueprint : blueprint.days()) {
            PlanDay day = plan.addDay(dayBlueprint.dayOfWeek(), dayBlueprint.name());
            for (ExerciseSlot slot : dayBlueprint.exercises()) {
                Exercise exercise = exercises.get(slot.exerciseId());
                if (exercise == null) {
                    throw new IllegalStateException("Unknown exercise id " + slot.exerciseId());
                }
                day.addExercise(exercise, slot.sets(), slot.repMin(), slot.repMax());
            }
        }
        return trainingPlanRepository.saveAndFlush(plan);
    }
}
