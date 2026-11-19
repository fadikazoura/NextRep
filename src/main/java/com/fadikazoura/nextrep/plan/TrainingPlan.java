package com.fadikazoura.nextrep.plan;

import com.fadikazoura.nextrep.user.User;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "training_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlanSource source;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanDay> days = new ArrayList<>();

    public TrainingPlan(User user, String name, PlanSource source) {
        this.user = user;
        this.name = name;
        this.source = source;
    }

    public PlanDay addDay(DayOfWeek dayOfWeek, String dayName) {
        PlanDay day = new PlanDay(this, dayOfWeek, dayName);
        days.add(day);
        return day;
    }

    public List<PlanDay> getDays() {
        return Collections.unmodifiableList(days);
    }

    public void deactivate() {
        this.active = false;
    }
}
