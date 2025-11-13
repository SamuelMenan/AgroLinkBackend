package com.agrolink.infrastructure.reviews;

import com.agrolink.domain.reviews.Review;
import com.agrolink.domain.reviews.ReviewPersistence;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge implementor (concrete): stores reviews in-memory.
 */
public class InMemoryReviewPersistence implements ReviewPersistence {
    private final Map<UUID, Review> store = new ConcurrentHashMap<>();

    @Override
    public void persist(Review review) {
        store.put(review.id(), review);
    }

    public Review get(UUID id) {
        return store.get(id);
    }
}
