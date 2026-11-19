package com.fadikazoura.nextrep.feedback;

import java.time.Instant;

public record ContactMessageResponse(ContactTopic topic, String message, Instant createdAt) {

    public static ContactMessageResponse from(ContactMessage message) {
        return new ContactMessageResponse(message.getTopic(), message.getMessage(), message.getCreatedAt());
    }
}
