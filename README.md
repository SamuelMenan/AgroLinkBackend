# Despliegue en Render

Render no instala Maven automáticamente en un servicio de tipo Node. Para este backend Spring Boot debes usar:

1. Un servicio Web con Docker (recomendado) apuntando al directorio `AgroLinkBackend`.
2. Opcionalmente agregar Maven Wrapper y usar un servicio JVM, pero Docker es más directo.

## Opción Docker

Ya existe un `Dockerfile` multi-stage. Pasos:

1. Crear nuevo servicio en Render: Web Service -> seleccionar repositorio -> Root directory: `AgroLinkBackend`.
2. Render detectará el `Dockerfile` automáticamente. No definas build/start commands manuales.
3. Añade variable de entorno si deseas ajustar memoria/otras configs. El puerto lo inyecta Render como `PORT` y se mapea gracias a `server.port=${PORT:8080}` en `application.yml`.
4. Deploy.

## Opción Maven Wrapper (alternativa)

Si prefieres sin Docker:

```bash
mvn -N wrapper
git add mvnw mvnw.cmd .mvn
git commit -m "Add Maven wrapper"
git push
```

Luego en Render crea servicio tipo "Web Service" (no Node) y usa:

- Build command: `./mvnw -q -DskipTests package`
- Start command: `java -jar target/agrolink-backend-0.0.1-SNAPSHOT.jar`

## JAVA_OPTS

Puedes ajustar el heap si lo necesitas:

```bash
JAVA_OPTS=-Xms128m -Xmx512m
```

## Salud

Actuator expone `/actuator/health` (incluido por `spring-boot-starter-actuator`).

## OpenAPI

`springdoc` habilita la UI en `/swagger-ui.html`.

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