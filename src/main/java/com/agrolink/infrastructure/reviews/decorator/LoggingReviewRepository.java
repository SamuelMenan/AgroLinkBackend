package com.agrolink.infrastructure.reviews.decorator;

import com.agrolink.domain.reviews.Review;
import com.agrolink.domain.reviews.ReviewRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Concrete Decorator: adds simple logging around repository calls.
 */
public class LoggingReviewRepository extends ReviewRepositoryDecorator {

    public LoggingReviewRepository(ReviewRepository delegate) {
        super(delegate);
    }

    @Override
    public Review save(Review review) {
        System.out.println("[" + Instant.now() + "] save review id=" + review.id());
        return super.save(review);
    }

    @Override
    public List<Review> findByProductId(UUID productId) {
        System.out.println("[" + Instant.now() + "] findByProductId productId=" + productId);
        return super.findByProductId(productId);
    }
}
