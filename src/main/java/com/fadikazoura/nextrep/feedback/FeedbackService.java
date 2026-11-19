package com.fadikazoura.nextrep.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    /** Simple spam protection: at most this many contact messages per user in 24 hours. */
    static final int MESSAGES_PER_DAY = 5;

    private final AppRatingRepository appRatingRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RatingResponse getRating(Long userId) {
        return appRatingRepository.findById(userId).map(RatingResponse::from).orElseThrow(RatingNotFoundException::new);
    }

    /** Rating again replaces the previous rating (one per user). */
    @Transactional
    public RatingResponse rate(Long userId, RatingRequest request) {
        AppRating rating = appRatingRepository.findById(userId).orElseGet(() -> new AppRating(userId));
        rating.rate(request.stars(), blankToNull(request.comment()));
        return RatingResponse.from(appRatingRepository.saveAndFlush(rating));
    }

    @Transactional
    public void contact(Long userId, ContactRequest request) {
        long recent = contactMessageRepository.countByUserIdAndCreatedAtAfter(userId, clock.instant().minus(Duration.ofDays(1)));
        if (recent >= MESSAGES_PER_DAY) throw new TooManyMessagesException();
        ContactMessage saved = contactMessageRepository.save(new ContactMessage(userId, request.topic(), request.message().strip()));
        log.info("New contact message #{} ({}) from user {}", saved.getId(), saved.getTopic(), userId);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
