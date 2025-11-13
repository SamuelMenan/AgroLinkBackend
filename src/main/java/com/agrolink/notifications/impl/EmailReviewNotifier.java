package com.agrolink.notifications.impl;

import com.agrolink.domain.reviews.Review;
import com.agrolink.notifications.ReviewNotifier;

public class EmailReviewNotifier implements ReviewNotifier {
    @Override
    public void notifyCreated(Review review) {
        // stub: send email notification
        System.out.println("[Email] Review created: " + review.id());
    }
}
