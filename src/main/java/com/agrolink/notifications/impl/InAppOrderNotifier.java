package com.agrolink.notifications.impl;

import com.agrolink.notifications.OrderNotifier;

import java.util.UUID;

public class InAppOrderNotifier implements OrderNotifier {
    @Override
    public void notifyStatusChanged(UUID orderId, String newStatus) {
        // stub: push in-app notification
        System.out.println("[InApp] Order " + orderId + " status: " + newStatus);
    }
}
