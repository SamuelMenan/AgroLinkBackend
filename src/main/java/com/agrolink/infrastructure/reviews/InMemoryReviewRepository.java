package com.agrolink.infrastructure.reviews;

import com.agrolink.domain.reviews.Review;
import com.agrolink.domain.reviews.ReviewRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryReviewRepository implements ReviewRepository {
    private final Map<UUID, Review> store = new ConcurrentHashMap<>();

    @Override
    public Review save(Review review) {
        store.put(review.id(), review);
        return review;
    }

    @Override
    public List<Review> findByProductId(UUID productId) {
        List<Review> result = new ArrayList<>();
        for (Review r : store.values()) {
            if (r.productId().equals(productId)) result.add(r);
        }
        return result;
    }
}
