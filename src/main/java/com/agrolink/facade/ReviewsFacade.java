package com.agrolink.facade;

import com.agrolink.domain.reviews.Review;
import com.agrolink.domain.reviews.ReviewDraftBuilder;
import com.agrolink.domain.reviews.ReviewRepository;
import com.agrolink.domain.reviews.ReviewService;

import java.util.List;
import java.util.UUID;

/**
 * Facade pattern: simplified API for review operations.
 * Hides builder, pipeline, notifications, persistence behind easy methods.
 */
public class ReviewsFacade {
    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    public ReviewsFacade(ReviewService reviewService, ReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
    }

    public Review createReview(UUID productId, UUID userId, int rating, String comment) {
        var draft = ReviewDraftBuilder.builder()
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .comment(comment)
                .build();
        return reviewService.createReview(draft);
    }

    public List<Review> listProductReviews(UUID productId) {
        return reviewRepository.findByProductId(productId);
    }
}
