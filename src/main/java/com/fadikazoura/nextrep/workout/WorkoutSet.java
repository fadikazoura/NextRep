package com.fadikazoura.nextrep.workout;

import com.fadikazoura.nextrep.exercise.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "workout_sets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Column(nullable = false)
    private short setNumber;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private short reps;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    WorkoutSet(WorkoutSession session, Exercise exercise, int setNumber, BigDecimal weightKg, int reps) {
        this.session = session;
        this.exercise = exercise;
        this.setNumber = (short) setNumber;
        this.weightKg = weightKg;
        this.reps = (short) reps;
    }
}
