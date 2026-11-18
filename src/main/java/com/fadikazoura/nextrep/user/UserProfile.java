package com.fadikazoura.nextrep.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @Setter(AccessLevel.NONE)
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @Setter(AccessLevel.NONE)
    private User user;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    @Column(nullable = false)
    private short heightCm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingGoal goal;

    @Column(nullable = false)
    private short sessionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private WeightUnit weightUnit = WeightUnit.KG;

    private String injuries;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_training_days", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private Set<DayOfWeek> trainingDays = EnumSet.noneOf(DayOfWeek.class);

    @UpdateTimestamp
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public UserProfile(User user,
                       LocalDate birthDate,
                       Sex sex,
                       short heightCm,
                       ExperienceLevel experienceLevel,
                       TrainingGoal goal,
                       short sessionMinutes,
                       Equipment equipment,
                       Set<DayOfWeek> trainingDays) {
        this.user = user;
        this.birthDate = birthDate;
        this.sex = sex;
        this.heightCm = heightCm;
        this.experienceLevel = experienceLevel;
        this.goal = goal;
        this.sessionMinutes = sessionMinutes;
        this.equipment = equipment;
        this.trainingDays = EnumSet.copyOf(trainingDays);
    }
    public void replaceTrainingDays(Set<DayOfWeek> days) {
        trainingDays.clear();
        trainingDays.addAll(days);
    }

}
