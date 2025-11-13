package com.agrolink.infrastructure.supabase;

import com.agrolink.domain.reviews.Review;
import com.agrolink.domain.reviews.ReviewPersistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter: adapts a Supabase HTTP client to the ReviewPersistence interface.
 */
public class SupabaseClientAdapter implements ReviewPersistence {
    private final SupabaseHttpClient client;

    public SupabaseClientAdapter(SupabaseHttpClient client) {
        this.client = client;
    }

    @Override
    public void persist(Review review) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", review.id().toString());
        payload.put("product_id", review.productId().toString());
        payload.put("user_id", review.userId().toString());
        payload.put("rating", review.rating());
        payload.put("comment", review.comment());
        payload.put("created_at", review.createdAt().toString());
        client.insert("reviews", payload);
    }
}
