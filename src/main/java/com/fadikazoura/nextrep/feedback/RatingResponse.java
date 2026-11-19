package com.fadikazoura.nextrep.feedback;

import java.time.Instant;

public record RatingResponse(int stars, String comment, Instant updatedAt) {

    public static RatingResponse from(AppRating rating) {
        return new RatingResponse(rating.getStars(), rating.getComment(), rating.getUpdatedAt());
    }
}
