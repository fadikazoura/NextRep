package com.fadikazoura.nextrep.insights;

import com.fadikazoura.nextrep.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final JdbcTemplate jdbcTemplate;
    private final UserProfileRepository userProfileRepository;
    private final Clock clock;

    /**
     * @param zone     the user's time zone: a workout at 00:30 in Israel belongs to that day, not to yesterday (UTC)
     * @param firstDay the first day of the week (Monday in most of the world, Sunday in Israel)
     */
    @Transactional(readOnly = true)
    public InsightsResponse insights(Long userId, ZoneId zone, DayOfWeek firstDay) {
        int weeklyGoal = userProfileRepository.findById(userId)
                .map(profile -> profile.getTrainingDays().size())
                .orElse(0);
        return InsightsCalculator.calculate(completedSessions(userId, zone), weeklyGoal, LocalDate.now(clock.withZone(zone)),
                firstDay);
    }

    /** One row per completed workout, totals computed by the database (one query, no N+1). */
    private List<SessionTotals> completedSessions(Long userId, ZoneId zone) {
        return jdbcTemplate.query("""
                SELECT ws.started_at,
                       count(s.id)                  AS sets,
                       sum(s.weight_kg * s.reps)    AS volume,
                       max(s.weight_kg)             AS heaviest
                FROM workout_sessions ws
                JOIN workout_sets s ON s.session_id = ws.id
                WHERE ws.user_id = ? AND ws.completed_at IS NOT NULL
                GROUP BY ws.id, ws.started_at
                ORDER BY ws.started_at
                """,
                (rs, row) -> new SessionTotals(
                        rs.getTimestamp("started_at").toInstant().atZone(zone).toLocalDate(),
                        rs.getInt("sets"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("heaviest")),
                userId);
    }
}
