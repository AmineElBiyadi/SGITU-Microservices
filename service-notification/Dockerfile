# ── ÉTAPE 1 : image minimale JRE ────────────────────────────
FROM eclipse-temurin:21-jre

# ── ÉTAPE 2 : dossier de travail ────────────────────────────
WORKDIR /app

# ── ÉTAPE 3 : créer un utilisateur non-root ─────────────────
RUN addgroup --system appgroup && \
    adduser --system --ingroup appgroup --no-create-home appuser

# ── ÉTAPE 4 : copier le JAR explicitement ───────────────────
# Remplace par le nom exact généré par ton pom.xml
COPY target/notification-service-1.0.0.jar app.jar

# ── ÉTAPE 5 : donner les droits ─────────────────────────────
RUN chown appuser:appgroup app.jar

# ── ÉTAPE 6 : basculer sur l'utilisateur non-root ───────────
USER appuser

# ── ÉTAPE 7 : port exposé ───────────────────────────────────
EXPOSE 8085

# ── ÉTAPE 8 : healthcheck pour Docker Compose ───────────────
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/api/notifications/health || exit 1

# ── ÉTAPE 9 : lancer l'application ──────────────────────────
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]