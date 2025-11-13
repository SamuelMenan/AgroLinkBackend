package com.agrolink.notifications.impl;

import com.agrolink.domain.reviews.Review;
import com.agrolink.notifications.ReviewNotifier;

public class InAppReviewNotifier implements ReviewNotifier {
    @Override
    public void notifyCreated(Review review) {
        // stub: push in-app notification
        System.out.println("[InApp] Review created: " + review.id());
    }
}
