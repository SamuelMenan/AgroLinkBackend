package com.agrolink.config;

import com.agrolink.domain.reviews.*;
import com.agrolink.infrastructure.reviews.InMemoryReviewRepository;
import com.agrolink.infrastructure.reviews.decorator.LoggingReviewRepository;
import com.agrolink.infrastructure.supabase.SupabaseHttpClient;
import com.agrolink.moderation.ModerationRuleFactory;
import com.agrolink.moderation.ProfanityDictionary;
import com.agrolink.notifications.DefaultNotificationFactory;
import com.agrolink.notifications.NotificationFactory;
import com.agrolink.shared.DefaultTimeProvider;
import com.agrolink.shared.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

	@Bean
	public TimeProvider timeProvider() { return new DefaultTimeProvider(); }

	@Bean
	public ProfanityDictionary profanityDictionary() { return ProfanityDictionary.getInstance(); }

	@Bean
	public ModerationRuleFactory moderationRuleFactory() { return new ModerationRuleFactory(); }

	@Bean
	public ReviewModerationPipeline reviewModerationPipeline(ModerationRuleFactory factory) {
		return new DefaultReviewModerationPipeline(factory);
	}

	@Bean
	public NotificationFactory notificationFactory() { return new DefaultNotificationFactory(); }

	@Bean
	public ReviewRepository reviewRepository() {
		return new LoggingReviewRepository(new InMemoryReviewRepository());
	}

	// Using in-memory persistence by default; swap for Supabase adapter in prod
	@Bean
	public ReviewPersistence reviewPersistence() {
		// Placeholder: In-memory path handled inside service via repository; adapter example below
		// return new SupabaseClientAdapter(realSupabaseHttpClient());
		return review -> { /* no-op persistence for now, repository acts as storage */ };
	}

	// Example client bean if integrating Supabase later
	public SupabaseHttpClient realSupabaseHttpClient() { return (table, payload) -> { /* call external */ }; }

	@Bean
	public ReviewService reviewService(ReviewModerationPipeline pipeline,
									   ReviewRepository repository,
									   ReviewPersistence persistence,
									   ProfanityDictionary dictionary,
									   TimeProvider timeProvider,
									   NotificationFactory notificationFactory) {
		return new ReviewService(pipeline, repository, persistence, dictionary, timeProvider, notificationFactory);
	}

	@Bean
	public com.agrolink.facade.ReviewsFacade reviewsFacade(ReviewService reviewService,
														   ReviewRepository reviewRepository) {
		return new com.agrolink.facade.ReviewsFacade(reviewService, reviewRepository);
	}
}
