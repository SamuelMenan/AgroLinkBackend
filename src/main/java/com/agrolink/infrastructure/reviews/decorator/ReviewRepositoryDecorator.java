package com.agrolink.infrastructure.reviews.decorator;

import com.agrolink.domain.reviews.Review;
import com.agrolink.domain.reviews.ReviewRepository;

import java.util.List;
import java.util.UUID;

/**
 * Base Decorator for ReviewRepository. Forwards calls to a delegate.
 */
public abstract class ReviewRepositoryDecorator implements ReviewRepository {
    protected final ReviewRepository delegate;

    protected ReviewRepositoryDecorator(ReviewRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Review save(Review review) {
        return delegate.save(review);
    }

    @Override
    public List<Review> findByProductId(UUID productId) {
        return delegate.findByProductId(productId);
    }
}
