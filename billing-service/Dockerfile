FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="ragllm-platform"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY target/billing-service-*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
