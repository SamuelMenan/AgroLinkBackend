package com.agrolink.notifications;

import java.util.UUID;

public interface OrderNotifier {
    void notifyStatusChanged(UUID orderId, String newStatus);
}
