package com.agrolink.domain.reviews;

import java.util.UUID;

/**
 * Prototype pattern: allows cloning a base ReviewDraft and creating variants.
 */
public class ReviewDraftPrototype {
    private final ReviewDraft base;

    public ReviewDraftPrototype(ReviewDraft base) {
        if (base == null) throw new IllegalArgumentException("base draft cannot be null");
        this.base = base;
    }

    public ReviewDraft copy() {
        return new ReviewDraft(base.productId(), base.userId(), base.rating(), base.comment());
    }

    public ReviewDraft copyWith(UUID productId, UUID userId, Integer rating, String comment) {
        UUID p = productId != null ? productId : base.productId();
        UUID u = userId != null ? userId : base.userId();
        int r = rating != null ? rating : base.rating();
        String c = comment != null ? comment : base.comment();
        return new ReviewDraft(p, u, r, c);
    }
}
