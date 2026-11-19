package com.fadikazoura.nextrep.stats;

import com.fadikazoura.nextrep.exercise.Exercise;
import com.fadikazoura.nextrep.exercise.ExerciseNotFoundException;
import com.fadikazoura.nextrep.exercise.ExerciseRepository;
import com.fadikazoura.nextrep.stats.PersonalRecordResponse.RecordSet;
import com.fadikazoura.nextrep.workout.SetSummary;
import com.fadikazoura.nextrep.workout.WorkoutSet;
import com.fadikazoura.nextrep.workout.WorkoutSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final WorkoutSetRepository workoutSetRepository;
    private final ExerciseRepository exerciseRepository;
    private final PersonalRecordCalculator calculator;

    /** Records for every exercise the user has completed at least once, sorted by exercise name. */
    public List<PersonalRecordResponse> personalRecords(Long userId) {
        Map<Long, List<LoggedSet>> setsByExercise = workoutSetRepository.findCompletedSetSummaries(userId).stream()
                .collect(Collectors.groupingBy(SetSummary::getExerciseId,
                        Collectors.mapping(StatsService::fromSummary, Collectors.toList())));

        Map<Long, Exercise> exercises = exerciseRepository.findAllById(setsByExercise.keySet()).stream()
                .collect(Collectors.toMap(Exercise::getId, Function.identity()));

        return setsByExercise.entrySet().stream()
                .map(entry -> toResponse(exercises.get(entry.getKey()), calculator.records(entry.getValue())))
                .sorted(Comparator.comparing(PersonalRecordResponse::exerciseName))
                .toList();
    }

    public ExerciseProgressResponse progress(Long userId, Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));

        List<LoggedSet> sets = workoutSetRepository.findCompletedSetsOfExercise(userId, exerciseId).stream()
                .map(StatsService::fromSet)
                .toList();
        return new ExerciseProgressResponse(exercise.getId(), exercise.getName(), calculator.progress(sets));
    }

    private static PersonalRecordResponse toResponse(Exercise exercise, PersonalRecords records) {
        return new PersonalRecordResponse(exercise.getId(), exercise.getName(), exercise.getMuscleGroup(),
                RecordSet.from(records.heaviestSet()), RecordSet.from(records.bestEstimatedSet()),
                records.estimatedOneRepMaxKg(), RecordSet.from(records.mostRepsSet()));
    }

    private static LoggedSet fromSummary(SetSummary summary) {
        return new LoggedSet(toDate(summary.getFirstPerformedAt()), summary.getWeightKg(), summary.getReps());
    }

    private static LoggedSet fromSet(WorkoutSet set) {
        return new LoggedSet(toDate(set.getSession().getStartedAt()), set.getWeightKg(), set.getReps());
    }

    // Dates are in UTC for now; per-user time zones are on the backlog.
    private static LocalDate toDate(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}
