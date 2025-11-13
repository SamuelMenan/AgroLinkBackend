package com.agrolink.notifications.impl;

import com.agrolink.notifications.OrderNotifier;

import java.util.List;
import java.util.UUID;

public class CompositeOrderNotifier implements OrderNotifier {
    private final List<OrderNotifier> delegates;

    public CompositeOrderNotifier(List<OrderNotifier> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public void notifyStatusChanged(UUID orderId, String newStatus) {
        for (var d : delegates) d.notifyStatusChanged(orderId, newStatus);
    }
}
