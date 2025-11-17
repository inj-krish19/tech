# Start from an official Java 17 base image
FROM eclipse-temurin:21-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project files
COPY pom.xml ./
COPY src ./src
COPY uploads ./uploads
COPY .env /app/.env

RUN java -version

# Set the JAR file path
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "java -jar $JAR_FILE"]

# Expose the port the application will run on (9950)
EXPOSE 10000
