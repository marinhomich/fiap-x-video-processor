# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Estágio de Execução com FFmpeg instalado
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Instalar FFmpeg no container de runtime
RUN apk add --no-cache ffmpeg

# Criar usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/fiap-x-video-processor-1.0.0.jar app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
