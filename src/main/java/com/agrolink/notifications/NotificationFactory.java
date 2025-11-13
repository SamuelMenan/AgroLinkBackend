package com.agrolink.notifications;

public interface NotificationFactory {
    ReviewNotifier createReviewNotifier();
    OrderNotifier createOrderNotifier();
}
