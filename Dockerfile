# Multi-stage build para AgroLink Backend en Render
# Etapa de compilación
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copiamos pom primero para aprovechar cache de dependencias
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
# Copiamos el código fuente
COPY src ./src
# Construimos el jar (omitimos tests para acelerar; cambiar si quieres ejecutarlos)
RUN mvn -q -DskipTests package

# Etapa runtime minimal
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
ENV JAVA_OPTS=""
ENV PORT=8080
# Copiamos el jar construido
COPY --from=build /app/target/agrolink-backend-0.0.1-SNAPSHOT.jar app.jar
# Exponer puerto (informativo; Render usa PORT)
EXPOSE 8080
# Entrada
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
