package com.agrolink.notifications;

import com.agrolink.domain.reviews.Review;

public interface NotificationFactory {
    default void notifyReviewCreated(Review review) {
        // base stub
    }
}
