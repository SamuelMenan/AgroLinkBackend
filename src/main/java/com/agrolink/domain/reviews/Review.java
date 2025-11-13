package com.agrolink.domain.reviews;

import java.time.Instant;
import java.util.UUID;

public record Review(
        UUID id,
        UUID productId,
        UUID userId,
        int rating,
        String comment,
        Instant createdAt
) {}
