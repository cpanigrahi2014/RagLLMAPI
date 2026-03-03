FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="ragllm-platform"

# Install Tesseract OCR + English trained data for scanned PDF support
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-eng

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

COPY target/document-service-*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

# Tesseract data path for tess4j
ENV TESSDATA_PREFIX=/usr/share/tessdata

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
