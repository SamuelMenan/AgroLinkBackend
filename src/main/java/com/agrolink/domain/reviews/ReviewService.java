package com.agrolink.domain.reviews;

import com.agrolink.moderation.ProfanityDictionary;
import com.agrolink.shared.TimeProvider;

import java.time.Instant;
import java.util.UUID;

public class ReviewService {
    private final ReviewModerationPipeline pipeline;
    private final ReviewRepository repository;
    private final ReviewPersistence persistence;
    private final ProfanityDictionary dictionary;
    private final TimeProvider timeProvider;

    public ReviewService(ReviewModerationPipeline pipeline,
                         ReviewRepository repository,
                         ReviewPersistence persistence,
                         ProfanityDictionary dictionary,
                         TimeProvider timeProvider) {
        this.pipeline = pipeline;
        this.repository = repository;
        this.persistence = persistence;
        this.dictionary = dictionary;
        this.timeProvider = timeProvider;
    }

    public Review createReview(ReviewDraft draft) {
        var processed = pipeline.run(draft);
        // basic placeholder moderation check
        if (processed.comment() != null && dictionary.contains(processed.comment())) {
            throw new IllegalArgumentException("Comment contains prohibited content");
        }

        Instant now = timeProvider.now();
        Review review = new Review(UUID.randomUUID(), processed.productId(), processed.userId(), processed.rating(), processed.comment(), now);
        persistence.persist(review);
        return repository.save(review);
    }
}
