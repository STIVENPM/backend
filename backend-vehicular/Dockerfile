# Activa la sintaxis moderna de Dockerfile.
# syntax=docker/dockerfile:1

# Usa Java 17 con herramientas de compilación solo en esta etapa.
FROM eclipse-temurin:17-jdk-jammy AS build

# Carpeta de trabajo para compilar el proyecto.
WORKDIR /workspace

# Copia el soporte del wrapper de Maven.
COPY .mvn .mvn
# Copia el wrapper y las dependencias declaradas.
COPY mvnw pom.xml ./

# Descarga dependencias antes de copiar el código para reutilizar caché.
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests dependency:go-offline

# Copia el código fuente después de preparar dependencias.
COPY src src

# Ejecuta pruebas y genera el archivo JAR.
RUN ./mvnw -B test package


# Imagen final con Java 17, sin herramientas de compilación.
FROM eclipse-temurin:17-jre-jammy

# Carpeta donde se ejecuta la aplicación.
WORKDIR /app

# Trae solo el JAR generado en la etapa anterior.
COPY --from=build /workspace/target/*.jar app.jar

# Perfil predeterminado al ejecutar la imagen fuera de Compose.
ENV SPRING_PROFILES_ACTIVE=prod

# Documenta el puerto previsto para producción; Compose usa 8081.
EXPOSE 10000

# Inicia Spring Boot al arrancar el contenedor.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
