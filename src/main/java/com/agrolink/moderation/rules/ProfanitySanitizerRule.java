package com.agrolink.moderation.rules;

import com.agrolink.domain.reviews.ReviewDraft;
import com.agrolink.moderation.ModerationRule;
import com.agrolink.moderation.ProfanityDictionary;

/**
 * Sanitizes comment by masking profane words using ProfanityDictionary.
 */
public class ProfanitySanitizerRule implements ModerationRule {
    private final ProfanityDictionary dictionary = ProfanityDictionary.getInstance();

    @Override
    public ReviewDraft apply(ReviewDraft draft) {
        if (draft.comment() == null || draft.comment().isBlank()) return draft;
        String masked = dictionary.mask(draft.comment());
        if (masked.equals(draft.comment())) return draft;
        return new ReviewDraft(draft.productId(), draft.userId(), draft.rating(), masked);
    }
}
