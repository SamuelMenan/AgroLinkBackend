package com.agrolink.notifications;

import com.agrolink.notifications.impl.*;

import java.util.List;

public class DefaultNotificationFactory implements NotificationFactory {
    @Override
    public ReviewNotifier createReviewNotifier() {
        return new CompositeReviewNotifier(List.of(
                new EmailReviewNotifier(),
                new InAppReviewNotifier()
        ));
    }

    @Override
    public OrderNotifier createOrderNotifier() {
        return new CompositeOrderNotifier(List.of(
                new EmailOrderNotifier(),
                new InAppOrderNotifier()
        ));
    }
}
