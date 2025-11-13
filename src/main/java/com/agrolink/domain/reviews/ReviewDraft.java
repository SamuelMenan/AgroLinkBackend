package com.agrolink.domain.reviews;

import java.util.UUID;

public record ReviewDraft(
        UUID productId,
        UUID userId,
        int rating,
        String comment
) {}
