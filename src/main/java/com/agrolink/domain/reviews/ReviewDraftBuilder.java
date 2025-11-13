package com.agrolink.domain.reviews;

import java.util.Objects;
import java.util.UUID;

/**
 * Builder pattern: fluent construction of ReviewDraft with validation.
 */
public class ReviewDraftBuilder {
    private UUID productId;
    private UUID userId;
    private Integer rating;
    private String comment;

    public static ReviewDraftBuilder builder() {
        return new ReviewDraftBuilder();
    }

    public ReviewDraftBuilder productId(UUID productId) {
        this.productId = productId;
        return this;
    }

    public ReviewDraftBuilder userId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public ReviewDraftBuilder rating(int rating) {
        this.rating = rating;
        return this;
    }

    public ReviewDraftBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    public ReviewDraft build() {
        UUID p = Objects.requireNonNull(productId, "productId is required");
        UUID u = Objects.requireNonNull(userId, "userId is required");
        int r = Objects.requireNonNull(rating, "rating is required");
        if (r < 1 || r > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        return new ReviewDraft(p, u, r, comment);
    }
}
