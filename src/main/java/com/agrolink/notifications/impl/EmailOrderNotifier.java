package com.agrolink.notifications.impl;

import com.agrolink.notifications.OrderNotifier;

import java.util.UUID;

public class EmailOrderNotifier implements OrderNotifier {
    @Override
    public void notifyStatusChanged(UUID orderId, String newStatus) {
        // stub: send email notification for order status change
        System.out.println("[Email] Order " + orderId + " status: " + newStatus);
    }
}
