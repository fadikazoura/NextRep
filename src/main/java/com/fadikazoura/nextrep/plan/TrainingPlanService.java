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
import com.fadikazoura.nextrep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final DefaultPlanGenerator defaultPlanGenerator;
    private final SmartPlanGenerator smartPlanGenerator;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * Generates and saves a new active plan. With {@code useAi}, the AI planner is tried first and the
     * default generator is the fallback, so the user always gets a plan.
     * <p>
     * Deliberately not {@code @Transactional}: the AI call can take many seconds, and holding a database
     * transaction (and connection) open while waiting on a remote service would exhaust the pool under load.
     * The reads and the write each run in their own short transaction instead.
     */
    public TrainingPlanResponse generatePlan(Long userId, boolean useAi) {
        GenerationContext context = transactionTemplate.execute(status -> loadContext(userId));

        GeneratedPlan generated = (useAi ? tryAi(context) : Optional.<GeneratedPlan>empty())
                .orElseGet(() -> new GeneratedPlan(
                        defaultPlanGenerator.generate(context.input(), context.catalog()), PlanSource.DEFAULT));

        return transactionTemplate.execute(status ->
                TrainingPlanResponse.from(savePlan(userId, generated.blueprint(), generated.source())));
    }

    @Transactional(readOnly = true)
    public TrainingPlanResponse getActivePlan(Long userId) {
        return trainingPlanRepository.findByUserIdAndActiveTrue(userId)
                .map(TrainingPlanResponse::from)
                .orElseThrow(PlanNotFoundException::new);
    }

    private GenerationContext loadContext(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(ProfileNotFoundException::new);
        List<ExerciseResponse> catalog = exerciseRepository.findAll().stream()
                .map(ExerciseResponse::from)
                .toList();
        int age = Period.between(profile.getBirthDate(), LocalDate.now(clock)).getYears();
        return new GenerationContext(PlanGenerationInput.from(profile), age, profile.getInjuries(), catalog);
    }

    private Optional<GeneratedPlan> tryAi(GenerationContext context) {
        if (!smartPlanGenerator.isAvailable()) {
            log.info("AI planner is not configured; using the default generator");
            return Optional.empty();
        }
        try {
            PlanBlueprint blueprint = smartPlanGenerator.generate(
                    context.input(), context.age(), context.injuries(), context.catalog());
            return Optional.of(new GeneratedPlan(blueprint, PlanSource.AI));
        } catch (RuntimeException e) {
            // The AI is an optional upgrade: any failure (outage, timeout, invalid answer) falls back.
            log.warn("AI plan generation failed, using the default generator: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Saves a blueprint as the user's new active plan. Shared by the default generator and the AI planner.
     */
    TrainingPlan savePlan(Long userId, PlanBlueprint blueprint, PlanSource source) {
        trainingPlanRepository.findByUserIdAndActiveTrue(userId).ifPresent(current -> {
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

        User user = userRepository.getReferenceById(userId);
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

    private record GenerationContext(PlanGenerationInput input, int age, String injuries,
                                     List<ExerciseResponse> catalog) {
    }

    private record GeneratedPlan(PlanBlueprint blueprint, PlanSource source) {
    }
}
