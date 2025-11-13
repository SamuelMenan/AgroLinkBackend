package com.agrolink.notifications;

import com.agrolink.domain.reviews.Review;

public interface ReviewNotifier {
    void notifyCreated(Review review);
}
