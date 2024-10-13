# Usa una imagen oficial de Java 17 (puedes cambiar la versión si es necesario)
FROM openjdk:17-jdk-slim

# Establece el directorio de trabajo en el contenedor
WORKDIR /app

# Copia el archivo pom.xml y las dependencias (necesario para la construcción en Docker)
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn

# Copia el código fuente del proyecto
COPY src ./src

# Descarga las dependencias y construye el proyecto
RUN ./mvnw clean install -DskipTests

# Expone el puerto que utiliza tu aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación Spring Boot
CMD ["java", "-jar", "target/swgds-jucam-backend-0.0.1-SNAPSHOT.jar"]
