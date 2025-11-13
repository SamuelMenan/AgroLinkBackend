package com.agrolink.moderation.rules;

import com.agrolink.domain.reviews.ReviewDraft;
import com.agrolink.moderation.ModerationRule;

/**
 * Validates comment length.
 */
public class MaxLengthRule implements ModerationRule {
    private final int max;

    public MaxLengthRule(int max) {
        this.max = max;
    }

    @Override
    public ReviewDraft apply(ReviewDraft draft) {
        String c = draft.comment();
        if (c != null && c.length() > max) {
            throw new IllegalArgumentException("comment exceeds max length: " + max);
        }
        return draft;
    }
}
