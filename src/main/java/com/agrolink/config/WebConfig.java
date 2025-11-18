package com.agrolink.config;

import com.agrolink.domain.reviews.*;
import com.agrolink.infrastructure.reviews.InMemoryReviewRepository;
import com.agrolink.infrastructure.reviews.decorator.LoggingReviewRepository;
import com.agrolink.infrastructure.supabase.SupabaseClientAdapter;
import com.agrolink.infrastructure.supabase.SupabaseHttpClient;
import com.agrolink.infrastructure.supabase.SupabaseRestClient;
import com.agrolink.infrastructure.supabase.SupabasePostgrestService;
import com.agrolink.infrastructure.supabase.SupabaseStorageService;
import com.agrolink.moderation.ModerationRuleFactory;
import com.agrolink.moderation.ProfanityDictionary;
import com.agrolink.notifications.DefaultNotificationFactory;
import com.agrolink.notifications.NotificationFactory;
import com.agrolink.shared.DefaultTimeProvider;
import com.agrolink.shared.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.cdimascio.dotenv.Dotenv;

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

	// Using in-memory persistence by default; if SUPABASE_URL and SUPABASE_SERVICE_KEY are present,
	// wire the Supabase adapter automatically.
	@Bean
	public ReviewPersistence reviewPersistence() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		String url = EnvUtils.getenv(dotenv,"SUPABASE_URL");
		String key = EnvUtils.getenv(dotenv,"SUPABASE_SERVICE_KEY");
		if (url != null && !url.isBlank() && key != null && !key.isBlank()) {
			SupabaseHttpClient client = new SupabaseRestClient(url, key);
			return new SupabaseClientAdapter(client);
		}
		return review -> { /* no-op persistence for now, repository acts as storage */ };
	}

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

	@Bean
	public SupabasePostgrestService supabasePostgrestService() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		String url = EnvUtils.getenv(dotenv,"SUPABASE_URL");
		String anon = EnvUtils.getenv(dotenv,"SUPABASE_ANON_KEY");
		String service = EnvUtils.getenv(dotenv,"SUPABASE_SERVICE_KEY");
		if (url == null || url.isBlank() || anon == null || anon.isBlank() || service == null || service.isBlank()) {
			System.err.println("[WARN] Supabase PostgREST no configurado: define SUPABASE_URL, SUPABASE_ANON_KEY y SUPABASE_SERVICE_KEY. Se usarán respuestas 503.");
			return new SupabasePostgrestService("", "", "");
		}
		return new SupabasePostgrestService(url, anon, service);
	}

	@Bean
	public SupabaseStorageService supabaseStorageService() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		String url = EnvUtils.getenv(dotenv,"SUPABASE_URL");
		String service = EnvUtils.getenv(dotenv,"SUPABASE_SERVICE_KEY");
		if (url == null || url.isBlank() || service == null || service.isBlank()) {
			System.err.println("[WARN] Supabase Storage no configurado: define SUPABASE_URL y SUPABASE_SERVICE_KEY. Se devolverán respuestas 503.");
			return new SupabaseStorageService("", "");
		}
		return new SupabaseStorageService(url, service);
	}

	// CORS is configured in CorsConfig to avoid duplicate bean definitions.
}
