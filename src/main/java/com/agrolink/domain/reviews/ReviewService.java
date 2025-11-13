package com.agrolink.domain.reviews;

import com.agrolink.moderation.ProfanityDictionary;
import com.agrolink.notifications.NotificationFactory;
import com.agrolink.shared.TimeProvider;

import java.time.Instant;
import java.util.UUID;

public class ReviewService {
    private final ReviewModerationPipeline pipeline;
    private final ReviewRepository repository;
    private final ReviewPersistence persistence;
    private final ProfanityDictionary dictionary;
    private final TimeProvider timeProvider;
    private final NotificationFactory notificationFactory;

    public ReviewService(ReviewModerationPipeline pipeline,
                         ReviewRepository repository,
                         ReviewPersistence persistence,
                         ProfanityDictionary dictionary,
                         TimeProvider timeProvider,
                         NotificationFactory notificationFactory) {
        this.pipeline = pipeline;
        this.repository = repository;
        this.persistence = persistence;
        this.dictionary = dictionary;
        this.timeProvider = timeProvider;
        this.notificationFactory = notificationFactory;
    }

    public Review createReview(ReviewDraft draft) {
        var processed = pipeline.run(draft);
        if (processed.comment() != null && dictionary.contains(processed.comment())) {
            throw new IllegalArgumentException("Comment contains prohibited content");
        }

        Instant now = timeProvider.now();
        Review review = new Review(UUID.randomUUID(), processed.productId(), processed.userId(), processed.rating(), processed.comment(), now);
        persistence.persist(review);
        Review saved = repository.save(review);
        // notify via abstract factory-created notifier
        notificationFactory.createReviewNotifier().notifyCreated(saved);
        return saved;
    }
}
