# One image = React build + Spring Boot API on the same origin (the refresh cookie stays first-party).
#   docker build -t portfolio .
#   docker run -p 8080:8080 --env-file backend/.env portfolio
# All configuration comes from environment variables at run time; no secret is baked into the image.

# ---- 1. Build the React app -------------------------------------------------------------------
FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. Build the Spring Boot jar with the React files as static resources ----------------------
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /src
COPY backend/ ./
COPY --from=web /web/dist/ portfolio-app/src/main/resources/static/
RUN mvn -B -q -DskipTests -pl portfolio-app -am package

# ---- 3. Small runtime image, non-root user ----------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=api /src/portfolio-app/target/portfolio.jar app.jar
USER app
# Heap follows the container memory limit (Railway trial: 1 GB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
