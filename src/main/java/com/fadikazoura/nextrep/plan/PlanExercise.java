package com.fadikazoura.nextrep.plan;

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

@Entity
@Table(name = "plan_exercises")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_day_id")
    private PlanDay planDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Column(nullable = false)
    private short position;

    @Column(nullable = false)
    private short sets;

    @Column(nullable = false)
    private short repMin;

    @Column(nullable = false)
    private short repMax;

    PlanExercise(PlanDay planDay, Exercise exercise, int position, int sets, int repMin, int repMax) {
        this.planDay = planDay;
        this.exercise = exercise;
        this.position = (short) position;
        this.sets = (short) sets;
        this.repMin = (short) repMin;
        this.repMax = (short) repMax;
    }
}
