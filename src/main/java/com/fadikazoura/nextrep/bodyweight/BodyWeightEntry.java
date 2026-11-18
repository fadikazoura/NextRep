package com.fadikazoura.nextrep.bodyweight;

import com.fadikazoura.nextrep.user.User;
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
import java.time.LocalDate;

@Entity
@Table(name = "body_weight_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyWeightEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private LocalDate recordedOn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public BodyWeightEntry(User user, BigDecimal weightKg, LocalDate recordedOn) {
        this.user = user;
        this.weightKg = weightKg;
        this.recordedOn = recordedOn;
    }

    public void changeWeight(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }
}