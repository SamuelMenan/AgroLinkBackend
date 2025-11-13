package com.agrolink.moderation;

import com.agrolink.domain.reviews.ReviewDraft;

/**
 * Product interface for the Factory Method pattern.
 * A rule can validate or sanitize a ReviewDraft. It returns a possibly modified draft
 * or throws IllegalArgumentException if validation fails.
 */
public interface ModerationRule {
    ReviewDraft apply(ReviewDraft draft);
}
