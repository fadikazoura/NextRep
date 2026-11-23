package com.fadikazoura.nextrep.demo;

import com.fadikazoura.nextrep.auth.TokenResponse;
import com.fadikazoura.nextrep.auth.TokenService;
import com.fadikazoura.nextrep.exercise.ExerciseRepository;
import com.fadikazoura.nextrep.plan.TrainingPlanResponse;
import com.fadikazoura.nextrep.plan.TrainingPlanService;
import com.fadikazoura.nextrep.user.Equipment;
import com.fadikazoura.nextrep.user.ExperienceLevel;
import com.fadikazoura.nextrep.user.ProfileRequest;
import com.fadikazoura.nextrep.user.Sex;
import com.fadikazoura.nextrep.user.TrainingGoal;
import com.fadikazoura.nextrep.user.User;
import com.fadikazoura.nextrep.user.UserRepository;
import com.fadikazoura.nextrep.user.UserService;
import com.fadikazoura.nextrep.user.WeightUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * "Try the demo": creates a fresh, private account full of sample data and logs the visitor straight in.
 * <p>
 * Every visitor gets their own account (nobody sees anybody else's changes, and there is no shared password to
 * leak). The account is a normal user, so every feature works on it; it is deleted with all its data after
 * {@link #LIFETIME} by {@link #deleteExpiredAccounts()}.
 * <p>
 * The history is written with plain SQL in one batch because it has to be in the past: the JPA entities stamp
 * workouts with "now" when they are created, which is right for real use and wrong for sample data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoAccountService {

    static final Duration LIFETIME = Duration.ofHours(24);
    private static final LocalTime WORKOUT_START = LocalTime.of(16, 30); // UTC, i.e. early evening in Israel

    private final UserRepository userRepository;
    private final UserService userService;
    private final TrainingPlanService trainingPlanService;
    private final ExerciseRepository exerciseRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final RandomGenerator random = new SecureRandom();

    public TokenResponse createDemoAccount() {
        User user = transactionTemplate.execute(status -> createUser());
        userService.upsertProfile(user.getId(), new ProfileRequest(
                LocalDate.of(1999, 6, 15), Sex.MALE, 180, ExperienceLevel.INTERMEDIATE, TrainingGoal.HYPERTROPHY,
                60, Equipment.FULL_GYM, WeightUnit.KG,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), null));
        // Rule-based on purpose: instant and free, and a demo must not depend on the AI being available.
        TrainingPlanResponse plan = trainingPlanService.generatePlan(user.getId(), false);
        transactionTemplate.executeWithoutResult(status -> writeHistory(user.getId(), plan));
        log.info("Created demo account {}", user.getId());
        return tokenService.issueToken(user);
    }

    private User createUser() {
        String username;
        do {
            username = "demo_" + (10000 + random.nextInt(90000));
        } while (userRepository.existsByUsernameIgnoreCase(username));
        // .invalid is a reserved domain: these addresses can never receive email.
        String email = "demo-" + UUID.randomUUID() + "@demo.nextrep.invalid";
        // Nobody knows this password; the demo is entered only through the token returned above.
        User user = new User(username, email, passwordEncoder.encode(UUID.randomUUID().toString()));
        user.changeAvatar("kettlebell");
        user.markAsDemo(clock.instant().plus(LIFETIME));
        return userRepository.saveAndFlush(user);
    }

    private void writeHistory(Long userId, TrainingPlanResponse plan) {
        LocalDate today = LocalDate.now(clock);
        Map<Long, DemoHistory.Exercise> catalog = exerciseRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getId(),
                        e -> new DemoHistory.Exercise(e.getEquipment(), e.getMovementType(), e.getName())));

        List<Object[]> setRows = new ArrayList<>();
        for (DemoHistory.Workout workout : DemoHistory.workouts(plan, catalog, today, random)) {
            Instant started = workout.date().atTime(WORKOUT_START).toInstant(ZoneOffset.UTC)
                    .plusSeconds(random.nextInt(90) * 60L);
            Instant completed = started.plus(Duration.ofMinutes(50 + random.nextInt(20)));
            Long sessionId = jdbcTemplate.queryForObject("""
                    INSERT INTO workout_sessions (user_id, plan_day_id, started_at, completed_at)
                    VALUES (?, ?, ?, ?) RETURNING id
                    """, Long.class, userId, workout.planDayId(), Timestamp.from(started), Timestamp.from(completed));
            for (DemoHistory.Set set : workout.sets()) {
                setRows.add(new Object[] {sessionId, set.exerciseId(), set.setNumber(), set.weightKg(), set.reps(),
                        Timestamp.from(started)});
            }
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO workout_sets (session_id, exercise_id, set_number, weight_kg, reps, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, setRows);

        jdbcTemplate.batchUpdate("INSERT INTO body_weight_entries (user_id, weight_kg, recorded_on) VALUES (?, ?, ?)",
                DemoHistory.bodyWeights(today, random).stream()
                        .map(entry -> new Object[] {userId, entry.weightKg(), Date.valueOf(entry.date())})
                        .toList());
    }

    /** Hourly: removes expired demo accounts; every table that belongs to a user is deleted with it (ON DELETE CASCADE). */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public int deleteExpiredAccounts() {
        int deleted = jdbcTemplate.update("DELETE FROM users WHERE demo_expires_at < ?", Timestamp.from(clock.instant()));
        if (deleted > 0) log.info("Deleted {} expired demo accounts", deleted);
        return deleted;
    }
}
