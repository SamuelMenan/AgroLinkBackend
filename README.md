# AgroLink Backend (Base Scaffold)

Minimal Spring Boot project with domain-oriented structure.

Structure:
- `src/main/java/com/agrolink/config` – Spring configuration
- `src/main/java/com/agrolink/api` – REST controllers (`ReviewsController`, `OrdersController`)
- `src/main/java/com/agrolink/domain` – Core domain modules
  - `reviews` – `Review`, `ReviewDraft`, `ReviewRepository`, `ReviewService`, `ReviewPersistence`, `ReviewModerationPipeline`
  - `orders`, `products` – placeholders
- `src/main/java/com/agrolink/infrastructure/supabase` – `SupabaseClientAdapter`
- `src/main/java/com/agrolink/notifications` – `NotificationFactory`
- `src/main/java/com/agrolink/moderation` – `ProfanityDictionary`, `ModerationRuleFactory`
- `src/main/java/com/agrolink/shared` – `TimeProvider`, `DefaultTimeProvider`

Quick start:
1. Build
```
mvn -DskipTests package
```
2. Run
```
mvn spring-boot:run
```

OpenAPI UI at `/swagger-ui.html` (springdoc).