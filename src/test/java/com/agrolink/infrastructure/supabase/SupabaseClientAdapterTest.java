package com.agrolink.infrastructure.supabase;

import com.agrolink.domain.reviews.Review;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseClientAdapterTest {

    @Test
    void adapterBuildsPayloadAndCallsClient() {
        AtomicReference<String> capturedTable = new AtomicReference<>();
        AtomicReference<java.util.Map<String, Object>> capturedPayload = new AtomicReference<>();

        SupabaseHttpClient fake = (table, payload) -> {
            capturedTable.set(table);
            capturedPayload.set(payload);
        };

        var adapter = new SupabaseClientAdapter(fake);
        var review = new Review(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 5, "ok", Instant.EPOCH);
        adapter.persist(review);

        assertEquals("reviews", capturedTable.get());
        assertNotNull(capturedPayload.get());
        assertEquals(review.rating(), capturedPayload.get().get("rating"));
    }
}
