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

# Pasta do volume compartilhado criada com o dono certo: volumes vazios herdam o dono
# do diretório da imagem; sem isso o Docker cria como root e o appuser não consegue gravar
RUN mkdir -p /app/storage_data && chown -R appuser:appgroup /app
USER appuser

COPY --from=builder /app/target/fiap-x-video-processor-1.0.0.jar app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
