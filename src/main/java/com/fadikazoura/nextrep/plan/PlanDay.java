package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.exercise.Exercise;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "plan_days")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private TrainingPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false, length = 50)
    private String name;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<PlanExercise> exercises = new ArrayList<>();

    PlanDay(TrainingPlan plan, DayOfWeek dayOfWeek, String name) {
        this.plan = plan;
        this.dayOfWeek = dayOfWeek;
        this.name = name;
    }

    public PlanExercise addExercise(Exercise exercise, int sets, int repMin, int repMax) {
        if (repMin > repMax) {
            throw new IllegalArgumentException("repMin must not be greater than repMax");
        }
        PlanExercise planExercise = new PlanExercise(this, exercise, exercises.size() + 1, sets, repMin, repMax);
        exercises.add(planExercise);
        return planExercise;
    }

    public List<PlanExercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }
}
