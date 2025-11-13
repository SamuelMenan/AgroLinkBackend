package com.agrolink.notifications.impl;

import com.agrolink.domain.reviews.Review;
import com.agrolink.notifications.ReviewNotifier;

import java.util.List;

public class CompositeReviewNotifier implements ReviewNotifier {
    private final List<ReviewNotifier> delegates;

    public CompositeReviewNotifier(List<ReviewNotifier> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void notifyCreated(Review review) {
        for (var d : delegates) d.notifyCreated(review);
    }
}
