package com.agrolink.domain.reviews;

import com.agrolink.moderation.ModerationRule;
import com.agrolink.moderation.ModerationRuleFactory;
import com.agrolink.moderation.rules.MaxLengthRule;
import com.agrolink.moderation.rules.ProfanitySanitizerRule;

import java.util.List;

public class DefaultReviewModerationPipeline extends ReviewModerationPipeline {
    private final List<ModerationRule> rules;

    public DefaultReviewModerationPipeline(ModerationRuleFactory factory) {
        this.rules = factory.createDefaultRules();
    }

    @Override
    protected ReviewDraft preprocess(ReviewDraft draft) {
        String c = draft.comment();
        if (c == null) return draft;
        String trimmed = c.trim();
        if (trimmed.equals(c)) return draft;
        return new ReviewDraft(draft.productId(), draft.userId(), draft.rating(), trimmed);
    }

    @Override
    protected void validate(ReviewDraft draft) {
        for (var r : rules) {
            if (r instanceof MaxLengthRule) {
                r.apply(draft);
            }
        }
    }

    @Override
    protected ReviewDraft sanitize(ReviewDraft draft) {
        ReviewDraft current = draft;
        for (var r : rules) {
            if (r instanceof ProfanitySanitizerRule) {
                current = r.apply(current);
            }
        }
        return current;
    }
}
