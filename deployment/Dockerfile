# ====== Etapa 1: Build ======
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# Opcional: quita esto si arreglas los CRLF con .gitattributes (ver notas)
RUN apk add --no-cache dos2unix

COPY . .
RUN dos2unix gradlew && chmod +x gradlew

# Compila solo el módulo boot
RUN ./gradlew :app-service:clean :app-service:bootJar -x test --no-daemon

# ====== Etapa 2: Runtime ======
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia directa del artefacto (sin find/cp/grep)
COPY --from=build /workspace/applications/app-service/build/libs/solicitudes.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]