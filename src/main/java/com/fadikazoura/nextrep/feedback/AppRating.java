package com.fadikazoura.nextrep.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "app_ratings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppRating {

    @Id
    private Long userId;

    @Column(nullable = false)
    private short stars;

    @Column(length = 1000)
    private String comment;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public AppRating(Long userId) {
        this.userId = userId;
    }

    public void rate(int stars, String comment) {
        this.stars = (short) stars;
        this.comment = comment;
    }
}
