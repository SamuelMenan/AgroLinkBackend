package com.agrolink.api;

import com.agrolink.domain.reviews.Review;
import com.agrolink.facade.ReviewsFacade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/v1/reviews")
@Validated
public class ReviewsController {

    private final ReviewsFacade facade;

    public ReviewsController(ReviewsFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping
    public ResponseEntity<Review> create(@RequestBody CreateReviewRequest req) {
        Review created = facade.createReview(req.productId(), req.userId(), req.rating(), req.comment());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/product/{productId}")
    public List<Review> listByProduct(@PathVariable UUID productId) {
        return facade.listProductReviews(productId);
    }

    public record CreateReviewRequest(
            @NotNull UUID productId,
            @NotNull UUID userId,
            @Min(1) @Max(5) int rating,
            @NotBlank String comment
    ) {}
}
