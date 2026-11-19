package com.fadikazoura.nextrep.workout;

import com.fadikazoura.nextrep.exercise.Exercise;
import com.fadikazoura.nextrep.plan.PlanDay;
import com.fadikazoura.nextrep.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "workout_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id")
    private PlanDay planDay;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<WorkoutSet> sets = new ArrayList<>();

    public WorkoutSession(User user, PlanDay planDay) {
        this.user = user;
        this.planDay = planDay;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    /** Adds the next set of an exercise; the set number continues from the last set of that exercise. */
    public WorkoutSet addSet(Exercise exercise, BigDecimal weightKg, int reps) {
        ensureInProgress();
        int nextSetNumber = sets.stream()
                .filter(set -> set.getExercise().getId().equals(exercise.getId()))
                .mapToInt(WorkoutSet::getSetNumber)
                .max()
                .orElse(0) + 1;
        WorkoutSet set = new WorkoutSet(this, exercise, nextSetNumber, weightKg, reps);
        sets.add(set);
        return set;
    }

    public void removeSet(Long setId) {
        ensureInProgress();
        boolean removed = sets.removeIf(set -> set.getId().equals(setId));
        if (!removed) {
            throw new WorkoutSetNotFoundException(setId);
        }
    }

    public void complete() {
        ensureInProgress();
        this.completedAt = Instant.now();
    }

    public List<WorkoutSet> getSets() {
        return Collections.unmodifiableList(sets);
    }

    private void ensureInProgress() {
        if (isCompleted()) {
            throw new WorkoutAlreadyCompletedException(id);
        }
    }
}
