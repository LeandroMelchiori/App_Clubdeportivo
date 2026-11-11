# Imagen de producción de la API. El jar lo arma Gradle antes de construirla:
#
#   ./gradlew :backend:bootJar
#   docker build --build-arg VERSION=v1.2.3 -t club-deportivo-api backend
#
# Las capas separan las dependencias del código, así una versión nueva solo cambia la última.

FROM eclipse-temurin:17-jre-noble AS capas
WORKDIR /build
COPY build/libs/club-deportivo-api.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extraido

FROM eclipse-temurin:17-jre-noble
ARG VERSION=dev
LABEL org.opencontainers.image.source="https://github.com/LeandroMelchiori/App_Clubdeportivo" \
      org.opencontainers.image.title="club-deportivo-api" \
      org.opencontainers.image.version="${VERSION}"
ENV CLUB_API_VERSION="${VERSION}" \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
WORKDIR /app
COPY --from=capas /build/extraido/dependencies/ ./
COPY --from=capas /build/extraido/spring-boot-loader/ ./
COPY --from=capas /build/extraido/snapshot-dependencies/ ./
COPY --from=capas /build/extraido/application/ ./
# Usuario sin privilegios y sin entrada en /etc/passwd: la imagen corre con el sistema de archivos de
# solo lectura y solo escribe en /tmp.
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
