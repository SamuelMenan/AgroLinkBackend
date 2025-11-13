package com.agrolink.domain.reviews;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository {
    Review save(Review review);
    List<Review> findByProductId(UUID productId);
}
