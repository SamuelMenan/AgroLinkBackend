package com.agrolink.domain.reviews;

public abstract class ReviewModerationPipeline {

    public final ReviewDraft run(ReviewDraft draft) {
        var pre = preprocess(draft);
        validate(pre);
        var sanitized = sanitize(pre);
        return sanitized;
    }

    protected ReviewDraft preprocess(ReviewDraft draft) {
        return draft;
    }

    protected void validate(ReviewDraft draft) {
        // default no-op validation
    }

    protected ReviewDraft sanitize(ReviewDraft draft) {
        return draft;
    }
}
